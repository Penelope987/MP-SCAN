import Foundation

public enum NetworkFailure {
    // Auth SDK wraps transport failures in FIRAuthErrorDomain/networkError (17020).
    // Expired/revoked credentials and denied permissions are deliberately excluded.
    public static func isTransient(_ error: Error) -> Bool {
        var current = error as NSError
        for _ in 0..<5 {
            if current.domain == NSURLErrorDomain { return true }
            if current.domain == "FIRAuthErrorDomain" { return current.code == 17020 }
            guard let nested = current.userInfo[NSUnderlyingErrorKey] as? NSError else { return false }
            current = nested
        }
        return false
    }
}
