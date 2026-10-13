import XCTest
final class MPSCANUITests: XCTestCase {
    func testMainNavigationOnIPhone() throws {
        let app = XCUIApplication(); app.launchArguments = ["--ui-testing"]; app.launch()
        XCTAssertTrue(app.staticTexts["Sua próxima história"].waitForExistence(timeout: 15))
        app.tabBars.buttons["Busca"].tap(); XCTAssertTrue(app.staticTexts["Encontre sua próxima leitura"].waitForExistence(timeout: 5))
        app.tabBars.buttons["Biblioteca"].tap(); XCTAssertTrue(app.staticTexts["Histórias para levar"].waitForExistence(timeout: 5))
        app.tabBars.buttons["Menu"].tap(); XCTAssertTrue(app.staticTexts["Aparência"].waitForExistence(timeout: 5))
    }
}
