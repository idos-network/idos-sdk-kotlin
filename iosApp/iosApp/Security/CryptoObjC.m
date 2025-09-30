#import "CryptoObjC.h"
#import "iosApp-Swift.h"

@implementation CryptoObjC

+ (NSData *)scryptWithPassword:(NSData *)password
                          salt:(NSData *)salt
                             n:(NSInteger)n
                             r:(NSInteger)r
                             p:(NSInteger)p
                         dkLen:(NSInteger)dkLen {
    return [CryptoHelperImpl scryptWithPassword:password
                                           salt:salt
                                              n:n
                                              r:r
                                              p:p
                                          dkLen:dkLen];
}

+ (NSData *)derivePublicKeyWithSecretKey:(NSData *)secretKey {
    return [CryptoHelperImpl derivePublicKeyWithSecretKey:secretKey];
}

+ (NSData *)encryptBoxWithMessage:(NSData *)message
                            nonce:(NSData *)nonce
                receiverPublicKey:(NSData *)receiverPublicKey
                  senderSecretKey:(NSData *)senderSecretKey {
    return [CryptoHelperImpl encryptBoxWithMessage:message
                                             nonce:nonce
                                 receiverPublicKey:receiverPublicKey
                                   senderSecretKey:senderSecretKey];
}

+ (NSData *)decryptBoxWithCiphertext:(NSData *)ciphertext
                               nonce:(NSData *)nonce
                       senderPublicKey:(NSData *)senderPublicKey
                    receiverSecretKey:(NSData *)receiverSecretKey {
    return [CryptoHelperImpl decryptBoxWithCiphertext:ciphertext
                                                nonce:nonce
                                        senderPublicKey:senderPublicKey
                                     receiverSecretKey:receiverSecretKey];
}

+ (NSData *)randomBytesWithCount:(NSInteger)count {
    return [CryptoHelperImpl randomBytesWithCount:count];
}

@end