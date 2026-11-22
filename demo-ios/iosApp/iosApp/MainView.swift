/*
 * Copyright 2019-2026 David Blanc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import SwiftUI
import HttpMockerDemo

// MARK: – ViewModel

@MainActor
final class MainViewModel: ObservableObject {

    enum MockMode: Int, CaseIterable, Identifiable {
        case disabled, enabled, mixed, record

        var id: Int { rawValue }

        var label: String {
            switch self {
            case .disabled: return "Disabled"
            case .enabled:  return "Enabled"
            case .mixed:    return "Mixed"
            case .record:   return "Record"
            }
        }

        var description: String {
            switch self {
            case .disabled:
                return "Interceptor is disabled.\n\nNetwork calls can go through (complete repo list and details)."
            case .enabled:
                return "Network calls are intercepted.\n\nRepo list is mocked (shortened list), details are not (details are in error)."
            case .mixed:
                return "Interceptor is in mixed mode.\n\nRepo list is mocked (shortened list), details are retrieved from a real call."
            case .record:
                return "Interceptor is recording.\n\nCalls are executed and saved in the app's Documents directory (visible in the Files app)."
            }
        }
    }

    struct RepoItem: Identifiable {
        let id: Int64
        let name: String
        let topContributor: String?
    }

    enum DataState {
        case empty
        case loading
        case success([RepoItem])
        case error(String)
    }

    @Published private(set) var mode: MockMode = .disabled
    @Published private(set) var data: DataState = .empty

    let recordPath: String

    private let demo: GithubDemo

    init() {
        let scenarioFolder = Bundle.main.url(forResource: "mocks", withExtension: nil)?.path ?? ""
        let documents = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let recordFolder = documents.appendingPathComponent("recorded").path
        demo = GithubDemo(scenarioFolder: scenarioFolder, recordFolder: recordFolder)
        recordPath = recordFolder
        mode = MockMode(rawValue: Int(demo.modeIndex)) ?? .disabled
    }

    func setMode(_ newMode: MockMode) {
        demo.setModeIndex(index: Int32(newMode.rawValue))
        mode = newMode
        data = .empty
    }

    func callService() {
        data = .loading
        demo.callService(callback: RepoCallback(
            onSuccess: { [weak self] repos in
                Task { @MainActor in self?.data = .success(repos) }
            },
            onError: { [weak self] message in
                Task { @MainActor in self?.data = .error(message) }
            }
        ))
    }
}

private final class RepoCallback: RepoListCallback {

    private let success: ([MainViewModel.RepoItem]) -> Void
    private let error: (String) -> Void

    init(onSuccess: @escaping ([MainViewModel.RepoItem]) -> Void, onError: @escaping (String) -> Void) {
        success = onSuccess
        error = onError
    }

    func onSuccess(repos: [Repo]) {
        success(repos.map { MainViewModel.RepoItem(id: $0.id, name: $0.name, topContributor: $0.topContributor) })
    }

    func onError(message: String) {
        error(message)
    }
}

// MARK: – Main screen

struct MainView: View {

    @StateObject private var viewModel = MainViewModel()

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 0) {
                Text("Mocking state")
                    .padding(8)

                ToggleGroup(selected: viewModel.mode, onSelect: viewModel.setMode)

                Text(viewModel.mode.description)
                    .padding(8)
                    .fixedSize(horizontal: false, vertical: true)

                Button {
                    viewModel.callService()
                } label: {
                    Text("Call service")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .padding(.vertical, 8)

                RepoList(data: viewModel.data)
            }
            .padding(.horizontal, 16)
            .tint(.demoPrimary)
            .navigationTitle("HttpMocker Demo")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    NavigationLink {
                        ScenarioTestsView()
                    } label: {
                        Label("Scenario tests", systemImage: "checklist")
                    }
                }
            }
        }
    }
}

// MARK: – Mode selector

struct ToggleGroup: View {
    let selected: MainViewModel.MockMode
    let onSelect: (MainViewModel.MockMode) -> Void

    var body: some View {
        Picker("Mocking state", selection: Binding(get: { selected }, set: onSelect)) {
            ForEach(MainViewModel.MockMode.allCases) { mode in
                Text(mode.label).tag(mode)
            }
        }
        .pickerStyle(.segmented)
    }
}

// MARK: – Repo list

struct RepoList: View {
    let data: MainViewModel.DataState

    var body: some View {
        switch data {
        case .empty:
            message("No result to display")
        case .loading:
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .error(let text):
            message(text)
        case .success(let repos):
            List(repos) { repo in
                VStack(alignment: .leading, spacing: 8) {
                    Text(repo.name)
                        .bold()
                    Text(repo.topContributor ?? "Error retrieving contributor")
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
                .padding(.vertical, 4)
            }
            .listStyle(.plain)
        }
    }

    private func message(_ text: String) -> some View {
        Text(text)
            .bold()
            .multilineTextAlignment(.center)
            .padding(16)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

extension Color {
    static let demoPrimary = Color(red: 0x00 / 255, green: 0x85 / 255, blue: 0x77 / 255)
}

#Preview {
    MainView()
}
