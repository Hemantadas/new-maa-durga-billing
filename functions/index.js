const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const crypto = require("node:crypto");
const { promisify } = require("node:util");
const scrypt = promisify(crypto.scrypt);

admin.initializeApp();
const db = admin.firestore();
const pinPepper = defineSecret("SHOP_PIN_PEPPER");

const MAX_FAILURES = 5;
const LOCK_MS = 15 * 60 * 1000;

function normalizeShopId(value) {
  const digits = String(value || "").replace(/\D/g, "");
  if (!/^\d{10}$/.test(digits)) {
    throw new HttpsError("invalid-argument", "দোকানের মোবাইল নম্বর সঠিক নয়");
  }
  return digits;
}

function safeEqualHex(a, b) {
  if (typeof a !== "string" || typeof b !== "string" ||
      !/^[a-f0-9]{128}$/i.test(a) || !/^[a-f0-9]{128}$/i.test(b)) return false;
  return crypto.timingSafeEqual(Buffer.from(a, "hex"), Buffer.from(b, "hex"));
}

async function makePinHash(pin, salt, pepper) {
  const derived = await scrypt(pin + pepper, salt, 64);
  return derived.toString("hex");
}

// The shopAuthIndex collection is private: clients cannot read or write it.
// Provision its records using the trusted owner-only bootstrap procedure.
exports.loginWithShopPin = onCall(
  {
    region: "asia-south1",
    enforceAppCheck: true,
    secrets: [pinPepper],
    maxInstances: 5
  },
  async (request) => {
    if (!request.app) {
      throw new HttpsError("failed-precondition", "এই অ্যাপটি যাচাই করা যায়নি");
    }

    const shopId = normalizeShopId(request.data && request.data.shopId);
    const pin = String((request.data && request.data.pin) || "");
    if (!/^\d{4,8}$/.test(pin)) {
      throw new HttpsError("invalid-argument", "PIN সঠিক নয়");
    }

    const pepper = pinPepper.value();
    const indexId = crypto.createHmac("sha256", pepper).update(shopId).digest("hex");
    const ref = db.collection("shopAuthIndex").doc(indexId);
    const now = Date.now();

    const authRecord = await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (!snap.exists) return null;
      const data = snap.data();
      if (Number(data.lockedUntil || 0) > now) {
        throw new HttpsError("resource-exhausted", "অনেকবার ভুল PIN হয়েছে। ১৫ মিনিট পরে চেষ্টা করুন");
      }

      const candidate = await makePinHash(pin, data.pinSalt, pepper);
      if (!safeEqualHex(candidate, data.pinHash)) {
        const failures = Number(data.failedAttempts || 0) + 1;
        tx.update(ref, {
          failedAttempts: failures >= MAX_FAILURES ? 0 : failures,
          lockedUntil: failures >= MAX_FAILURES ? now + LOCK_MS : 0,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        return { invalid: true };
      }

      tx.update(ref, {
        failedAttempts: 0,
        lockedUntil: 0,
        lastLoginAt: admin.firestore.FieldValue.serverTimestamp()
      });
      return { uid: data.uid };
    });

    if (!authRecord) {
      throw new HttpsError("unauthenticated", "Shop ID অথবা PIN সঠিক নয়");
    }
    if (authRecord.invalid) {
      throw new HttpsError("unauthenticated", "Shop ID অথবা PIN সঠিক নয়");
    }

    const token = await admin.auth().createCustomToken(authRecord.uid, {
      shopLogin: true
    });
    return { token };
  }
);
