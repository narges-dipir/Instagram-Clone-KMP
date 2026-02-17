import XCTest
@testable import iosApp

final class IOSAppSmokeTests: XCTestCase {
    func testContentViewCanBeCreated() {
        let view = ContentView()
        XCTAssertNotNil(view)
    }
}
