import Foundation
import idos_sdk

struct Credential: Codable, Identifiable {
    let id: String
    let type: String
    let level: String
    let status: String
    let issuer: String
    
    // Static factory method for creating from response
    static func from(response: GetCredentialsResponse) -> Credential {
        // Parse publicNotes JSON string to extract fields
        if let data = response.publicNotes.data(using: .utf8),
           let notes = try? JSONDecoder().decode(PublicNotes.self, from: data) {
            return Credential(
                id: response.id,
                type: notes.type,
                level: notes.level,
                status: notes.status,
                issuer: notes.issuer
            )
        } else {
            // Return with default values if parsing fails
            return Credential(
                id: response.id,
                type: "",
                level: "",
                status: "",
                issuer: ""
            )
        }
    }
}

struct CredentialDetail: Codable {
    let id: String
    let content: String
    let encryptorPublicKey: String
    
    // Static factory method for creating from response
    static func from(response: GetCredentialOwnedResponse) -> CredentialDetail {
        return CredentialDetail(
            id: response.id,
            content: response.content,
            encryptorPublicKey: response.encryptorPublicKey
        )
    }
}

private struct PublicNotes: Codable {
    let id: String
    let type: String
    let level: String
    let status: String
    let issuer: String
}
