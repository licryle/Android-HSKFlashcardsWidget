//
//  hskwidgetApp.swift
//  hskwidget
//
//  Created by Licryle on 9/10/25.
//

import SwiftUI
import crossPlatformKit

@main
struct hskwidgetApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView(viewModel: .init(loginRepository: LoginRepository(dataSource: LoginDataSource()), loginValidator: LoginDataValidator()))
        }
    }
}
