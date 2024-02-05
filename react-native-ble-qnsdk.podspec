require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name = "react-native-ble-qnsdk"
  s.version = package["version"]
  s.summary = package["description"]
  s.description = <<-DESC
    YolandaSync is a powerful Bluetooth communication library designed to simplify the process of synchronizing data with Yolanda scales. With a seamless integration into your app, YolandaSync allows you to effortlessly connect to Yolanda scales over Bluetooth and retrieve measurement data with just a few lines of code.

    Key Features:
    - Streamlined Bluetooth pairing and communication with Yolanda scales.
    - Retrieve precise weight and measurement data with ease.
    - Seamlessly integrate weight measurements into your app's workflow.

    GitHub: https://github.com/coachcare/react-native-ble-qnsdk
  DESC
  s.homepage = "https://github.com/coachcare/react-native-ble-qnsdk"
  s.license = "MIT"
  s.authors = { "Jeff Drakos" => "jeffdrakos@gmail.com" }
  s.platforms = { :ios => "11.0" }
  s.source = { :git => "https://github.com/coachcare/react-native-ble-qnsdk.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m,swift}"
  s.requires_arc = true

  # s.resources = 'src/assets/awaken180YolandoTestSdk.qn'
  s.resources = "src/assets/Awaken202401.qn"

  s.dependency "React"
  s.dependency "QNSDK", "2.9.0"
end
