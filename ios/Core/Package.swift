// swift-tools-version: 6.0
import PackageDescription
let package = Package(name: "MPSCANCore", platforms: [.iOS(.v17), .macOS(.v13)], products: [.library(name: "MPSCANCore", targets: ["MPSCANCore"])], dependencies: [.package(url: "https://github.com/scinfu/SwiftSoup.git", exact: "2.8.8")], targets: [.target(name: "MPSCANCore", dependencies: ["SwiftSoup"]), .testTarget(name: "MPSCANCoreTests", dependencies: ["MPSCANCore"])], swiftLanguageModes: [.v5])
