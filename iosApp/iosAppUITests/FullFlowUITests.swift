import XCTest

final class FullFlowUITests: XCTestCase {
    var app: XCUIApplication!

    let userDerivationPath = "m/44'/60'/0'/0/4"
    let mpcDerivationPath = "m/44'/60'/0'/0/3"

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["UI_TESTING", "RESET_STATE"]
        app.launch()
    }

    func testFullFlow_UserEnclave() throws {
        runFullFlowTest(derivationPath: userDerivationPath, isUserEnclave: true)
    }

    func testFullFlow_MpcEnclave() throws {
        runFullFlowTest(derivationPath: mpcDerivationPath, isUserEnclave: false)
    }

    private func runFullFlowTest(derivationPath: String, isUserEnclave: Bool) {
        XCTAssertTrue(app.staticTexts["Welcome"].waitForExistence(timeout: 5))
        app.buttons["Connect Wallet"].tap()

        XCTAssertTrue(app.staticTexts["Import Wallet"].waitForExistence(timeout: 5))

        let pathField = app.textFields["m/44'/60'/0'/0/47"].firstMatch
        pathField.tap()
        pathField.clearText()
        pathField.typeText(derivationPath)

        app.buttons["Import Wallet"].tap()

        XCTAssertTrue(app.staticTexts["Wallet Imported"].waitForExistence(timeout: 10))
        app.buttons["OK"].tap()

        sleep(2)

        if app.staticTexts["No credentials found"].exists {
            XCTFail("No credentials found - ensure test wallet has credentials")
            return
        }

        let firstCard = app.buttons["CredentialCard"].firstMatch
        XCTAssertTrue(firstCard.waitForExistence(timeout: 10))
        firstCard.tap()

        XCTAssertTrue(app.staticTexts["Credential Detail"].waitForExistence(timeout: 3))
        XCTAssertTrue(app.staticTexts["This credential is encrypted"].exists)

        app.buttons["Decrypt Content"].tap()

        // Wait for enclave dialog (password is auto-filled from Config in UI_TESTING mode)
        if isUserEnclave {
            XCTAssertTrue(app.staticTexts["Generate Encryption Key"].waitForExistence(timeout: 3))
        } else {
            XCTAssertTrue(app.staticTexts["Unlock MPC Enclave"].waitForExistence(timeout: 3))
        }

        let unlockButton = isUserEnclave ? "Generate Key" : "Unlock"
        app.buttons[unlockButton].tap()

        sleep(2)

        XCTAssertFalse(app.staticTexts["🔒 Content Encrypted"].exists)

        sleep(5)

        let contextExists = app.staticTexts.matching(NSPredicate(format: "label CONTAINS '@context'")).count > 0
        XCTAssertTrue(contextExists, "Expected '@context' in decrypted content")
    }
}

extension XCUIElement {
    func clearText() {
        guard let stringValue = self.value as? String else { return }
        let deleteString = String(repeating: XCUIKeyboardKey.delete.rawValue, count: stringValue.count)
        self.typeText(deleteString)
    }
}
