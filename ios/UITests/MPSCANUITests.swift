import XCTest
final class MPSCANUITests: XCTestCase {
    private func openApp() -> XCUIApplication {
        let app = XCUIApplication(); app.launchArguments = ["--ui-testing"]; app.launch()
        XCTAssertTrue(app.staticTexts["Sua próxima história"].waitForExistence(timeout: 15))
        return app
    }
    private func tapTab(_ app: XCUIApplication, _ title: String) {
        let tab = app.tabBars.buttons[title]
        if tab.exists { tab.tap() } else { app.buttons[title].firstMatch.tap() }
    }
    private func capture(_ app: XCUIApplication, _ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot()); attachment.name = name; attachment.lifetime = .keepAlways; add(attachment)
    }
    func testMainNavigation() throws {
        let app = openApp(); capture(app, "inicio")
        tapTab(app, "Busca"); XCTAssertTrue(app.staticTexts["Encontre sua próxima leitura"].waitForExistence(timeout: 5)); capture(app, "busca")
        tapTab(app, "Biblioteca"); XCTAssertTrue(app.staticTexts["Histórias para levar"].waitForExistence(timeout: 5)); capture(app, "biblioteca")
        tapTab(app, "Menu"); XCTAssertTrue(app.navigationBars["Ajustes"].waitForExistence(timeout: 5)); capture(app, "aparencia")
    }
    func testNovelAndMixedReader() throws {
        let app = openApp(); app.buttons["work-sample"].firstMatch.tap()
        XCTAssertTrue(app.buttons["chapter-sample-chapter"].waitForExistence(timeout: 10)); app.buttons["chapter-sample-chapter"].tap()
        XCTAssertTrue(app.staticTexts["Era uma vez uma história para levar com você, mesmo sem internet."].waitForExistence(timeout: 10)); capture(app, "leitor-misto")
    }
    func testOfflineDownloadAndReading() throws {
        let app = openApp(); app.buttons["work-sample"].firstMatch.tap()
        XCTAssertTrue(app.buttons["Baixar capítulo"].waitForExistence(timeout: 10)); app.buttons["Baixar capítulo"].tap()
        XCTAssertTrue(app.staticTexts["Disponível offline"].waitForExistence(timeout: 15))
        tapTab(app, "Biblioteca"); XCTAssertTrue(app.staticTexts["Capítulo 1"].waitForExistence(timeout: 10)); app.staticTexts["Capítulo 1"].tap()
        XCTAssertTrue(app.staticTexts["Era uma vez uma história para levar com você, mesmo sem internet."].waitForExistence(timeout: 10)); capture(app, "leitor-offline")
    }
}
