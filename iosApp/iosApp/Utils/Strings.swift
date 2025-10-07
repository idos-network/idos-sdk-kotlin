import Foundation

extension String {
    func removingPrefix(_ prefix: String) -> String {
        guard hasPrefix(prefix) else { return self }
        return String(dropFirst(prefix.count))
    }

    func addressNoPrefix() -> String {
        return removingPrefix("0x")
    }

    func addressWithPrefix() -> String {
        guard !hasPrefix("0x") else { return self }
        return "0x" + self
    }

    func prettyPrintedJSON() -> String {
        guard let data = self.data(using: .utf8),
              let jsonObject = try? JSONSerialization.jsonObject(with: data),
              let prettyData = try? JSONSerialization.data(withJSONObject: jsonObject, options: [.prettyPrinted, .sortedKeys]),
              let prettyString = String(data: prettyData, encoding: .utf8) else {
            return self
        }
        return prettyString
    }
}
