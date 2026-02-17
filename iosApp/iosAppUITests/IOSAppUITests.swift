import XCTest

final class IOSAppUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testMainTabsAreVisible() throws {
        let app = XCUIApplication()
        app.launch()

        XCTAssertTrue(app.buttons["Home"].waitForExistence(timeout: 10))
        XCTAssertTrue(app.buttons["Search"].exists)
        XCTAssertTrue(app.buttons["Reels"].exists)
        XCTAssertTrue(app.buttons["Profile"].exists)
    }

    func testCanNavigateToReelsAndProfile() throws {
        let app = XCUIApplication()
        app.launch()

        let reelsButton = app.buttons["Reels"]
        XCTAssertTrue(reelsButton.waitForExistence(timeout: 10))
        reelsButton.tap()

        let profileButton = app.buttons["Profile"]
        XCTAssertTrue(profileButton.exists)
        profileButton.tap()

        XCTAssertTrue(app.buttons["Profile"].exists)
    }
}
