import SwiftUI

/// JSON rendering components matching Android's dynamic JSON display

// MARK: - JSON Value Type
enum JSONValue {
    case object([String: JSONValue])
    case array([JSONValue])
    case string(String)
    case number(Double)
    case bool(Bool)
    case null

    init?(from any: Any?) {
        guard let any = any else {
            self = .null
            return
        }

        if let dict = any as? [String: Any] {
            var result: [String: JSONValue] = [:]
            for (key, value) in dict {
                if let jsonValue = JSONValue(from: value) {
                    result[key] = jsonValue
                }
            }
            self = .object(result)
        } else if let arr = any as? [Any] {
            let values = arr.compactMap { JSONValue(from: $0) }
            self = .array(values)
        } else if let str = any as? String {
            self = .string(str)
        } else if let num = any as? Double {
            self = .number(num)
        } else if let num = any as? Int {
            self = .number(Double(num))
        } else if let b = any as? Bool {
            self = .bool(b)
        } else if any is NSNull {
            self = .null
        } else {
            return nil
        }
    }
}

// MARK: - Main JSON Display View
struct JsonElementDisplay: View {
    let jsonValue: JSONValue

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            JsonElementContent(jsonValue: jsonValue, nestingLevel: 0)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

// MARK: - JSON Element Content (Recursive)
struct JsonElementContent: View {
    let jsonValue: JSONValue
    let nestingLevel: Int

    var body: some View {
        switch jsonValue {
        case .object(let dict):
            JsonObjectContent(jsonObject: dict, nestingLevel: nestingLevel)
        case .array(let arr):
            JsonArrayContent(jsonArray: arr, nestingLevel: nestingLevel)
        case .string(let str):
            JsonPrimitiveContent(value: "\"\(str)\"", color: .primary)
        case .number(let num):
            JsonPrimitiveContent(
                value: num.truncatingRemainder(dividingBy: 1) == 0 ? "\(Int(num))" : "\(num)",
                color: .purple
            )
        case .bool(let b):
            JsonPrimitiveContent(value: "\(b)", color: .orange)
        case .null:
            JsonPrimitiveContent(value: "null", color: .secondary)
        }
    }
}

// MARK: - JSON Object Content
struct JsonObjectContent: View {
    let jsonObject: [String: JSONValue]
    let nestingLevel: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(jsonObject.keys.sorted()), id: \.self) { key in
                if let value = jsonObject[key] {
                    JsonKeyValueItem(key: key, value: value, nestingLevel: nestingLevel)
                }
            }
        }
    }
}

// MARK: - JSON Key-Value Item
struct JsonKeyValueItem: View {
    let key: String
    let value: JSONValue
    let nestingLevel: Int

    private var indentationPadding: CGFloat {
        CGFloat(nestingLevel * 16)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Key label
            Text(key)
                .font(.caption)
                .fontWeight(.semibold)
                .foregroundColor(.blue)

            // Value content
            switch value {
            case .object(let dict):
                VStack(alignment: .leading, spacing: 8) {
                    JsonObjectContent(jsonObject: dict, nestingLevel: nestingLevel + 1)
                }
                .padding(8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.systemGray5))
                .cornerRadius(6)

            case .array(let arr):
                JsonArrayContent(jsonArray: arr, nestingLevel: nestingLevel + 1)

            default:
                JsonElementContent(jsonValue: value, nestingLevel: nestingLevel)
            }
        }
        .padding(.leading, indentationPadding)
    }
}

// MARK: - JSON Array Content
struct JsonArrayContent: View {
    let jsonArray: [JSONValue]
    let nestingLevel: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(jsonArray.enumerated()), id: \.offset) { index, item in
                JsonKeyValueItem(key: "[\(index)]", value: item, nestingLevel: nestingLevel)
            }
        }
        .padding(8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemGray5))
        .cornerRadius(6)
    }
}

// MARK: - JSON Primitive Content
struct JsonPrimitiveContent: View {
    let value: String
    let color: Color

    var body: some View {
        Text(value)
            .font(.body)
            .foregroundColor(color)
    }
}

// MARK: - Helper to parse JSON string
extension String {
    func parseJSON() -> JSONValue? {
        guard let data = self.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) else {
            return nil
        }
        return JSONValue(from: json)
    }
}
