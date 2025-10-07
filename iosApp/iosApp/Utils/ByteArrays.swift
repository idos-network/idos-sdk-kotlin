import Foundation
import idos_sdk

extension KotlinByteArray {
    // Convert KotlinByteArray to Data
    func toData() -> Data {
        var data = Data()
        for i in 0..<self.size {
            let byte = self.get(index: i)
            data.append(UInt8(bitPattern: byte))
        }
        return data
    }
    
    // Convert KotlinByteArray to [UInt8]
    func toByteArray() -> [UInt8] {
        var array = [UInt8]()
        for i in 0..<self.size {
            let byte = self.get(index: i)
            array.append(UInt8(bitPattern: byte))
        }
        return array
    }
}

extension Data {
    // Convert Data to KotlinByteArray
    func toKotlinByteArray() -> KotlinByteArray {
        let array = [UInt8](self)
        return array.toKotlinByteArray()
    }
}

extension Array where Element == UInt8 {
    // Convert [UInt8] to KotlinByteArray
    func toKotlinByteArray() -> KotlinByteArray {
        return KotlinByteArray(size: Int32(self.count)) { index in
            return KotlinByte(value: Int8(bitPattern: self[Int(truncating: index)]))
        }
    }
}
