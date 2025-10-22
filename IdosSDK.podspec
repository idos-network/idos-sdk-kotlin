Pod::Spec.new do |s|
  s.name         = 'IdosSDK'
  s.version      = '0.1.0'
  s.summary      = 'Kotlin Multiplatform SDK for idOS - Identity Operating System'
  s.description  = <<-DESC
                   Kotlin Multiplatform SDK for idOS providing Android, iOS, and JVM support.
                   Features include user management, wallet operations, credential handling,
                   access grants, and secure encryption with LOCAL and MPC modes.
                   DESC
  s.homepage     = 'https://github.com/idos-network/idos-sdk-kotlin'
  s.license      = { :type => 'MIT', :file => 'LICENSE' }
  s.authors      = { 'idOS Team' => 'dev@idos.network' }
  s.source       = {
    :http => 'https://github.com/idos-network/idos-sdk-kotlin/releases/download/v0.1.0/idos_sdk.xcframework.zip',
    :sha256 => 'CHECKSUM_PLACEHOLDER'
  }

  s.vendored_frameworks = 'idos_sdk.xcframework'
  s.platform     = :ios, '15.0'
  s.swift_version = '5.9'

  # Dependencies (if needed for consumers)
  # Uncomment if the XCFramework requires these at runtime
  # s.dependency 'Sodium', '~> 0.9.1'
  # s.dependency 'CryptoSwift', '~> 1.8.0'
end
