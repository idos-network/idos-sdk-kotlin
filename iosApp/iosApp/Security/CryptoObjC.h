#ifndef CryptoObjC_h
#define CryptoObjC_h

#import <Foundation/Foundation.h>

/**
 * Objective-C wrapper for crypto operations
 * This provides a C-compatible interface that Kotlin/Native can call via cinterop
 */
@interface CryptoObjC : NSObject

/**
 * Perform SCrypt key derivation
 */
+ (NSData * _Nullable)scryptWithPassword:(NSData * _Nonnull)password
                                    salt:(NSData * _Nonnull)salt
                                       n:(NSInteger)n
                                       r:(NSInteger)r
                                       p:(NSInteger)p
                                   dkLen:(NSInteger)dkLen;

/**
 * Derive public key from secret key using Curve25519
 */
+ (NSData * _Nullable)derivePublicKeyWithSecretKey:(NSData * _Nonnull)secretKey;

/**
 * Encrypt message using NaCl Box
 */
+ (NSData * _Nullable)encryptBoxWithMessage:(NSData * _Nonnull)message
                                      nonce:(NSData * _Nonnull)nonce
                          receiverPublicKey:(NSData * _Nonnull)receiverPublicKey
                          senderSecretKey:(NSData * _Nonnull)senderSecretKey;

/**
 * Decrypt message using NaCl Box
 */
+ (NSData * _Nullable)decryptBoxWithCiphertext:(NSData * _Nonnull)ciphertext
                                         nonce:(NSData * _Nonnull)nonce
                                 senderPublicKey:(NSData * _Nonnull)senderPublicKey
                              receiverSecretKey:(NSData * _Nonnull)receiverSecretKey;

/**
 * Generate random bytes
 */
+ (NSData * _Nonnull)randomBytesWithCount:(NSInteger)count;

@end

#endif /* CryptoObjC_h */