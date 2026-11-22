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
final class DemoViewModel: ObservableObject {

    @Published var results: [ResultItem] = []
    @Published var isRunning = false

    struct ResultItem: Identifiable {
        let id = UUID()
        let scenario: String
        let url: String
        let method: String
        let statusCode: Int
        let body: String
        let error: String

        var isError: Bool { statusCode == 0 }
        var statusText: String { isError ? "ERR" : "\(statusCode)" }
        var statusColor: Color {
            if isError { return .red }
            switch statusCode {
            case 200..<300: return .green
            case 300..<400: return .orange
            default:        return .red
            }
        }
    }

    func runScenarios() {
        isRunning = true
        results = []

        let runner = MockDemoRunner()
        let callback = SwiftResultCallback { [weak self] item in
            Task { @MainActor in
                self?.results.append(item)
            }
        } onDone: { [weak self] in
            Task { @MainActor in
                self?.isRunning = false
            }
        }
        runner.runAllScenarios(callback: callback)
    }
}

// MARK: – Callback bridge

private final class SwiftResultCallback: DemoResultCallback {

    private let onResult: (DemoViewModel.ResultItem) -> Void
    private let onDone: () -> Void

    init(onResult: @escaping (DemoViewModel.ResultItem) -> Void, onDone: @escaping () -> Void) {
        self.onResult = onResult
        self.onDone = onDone
    }

    func onResult(result: DemoResult) {
        let item = DemoViewModel.ResultItem(
            scenario: result.scenario,
            url: result.url,
            method: result.method,
            statusCode: Int(result.statusCode),
            body: result.body,
            error: result.error
        )
        onResult(item)
    }

    func onAllDone() {
        onDone()
    }
}

// MARK: – Views

struct ScenarioTestsView: View {

    @StateObject private var viewModel = DemoViewModel()

    var body: some View {
        Group {
            if viewModel.results.isEmpty && !viewModel.isRunning {
                emptyState
            } else {
                resultList
            }
        }
        .navigationTitle("Scenario tests")
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                runButton
            }
        }
    }

    // MARK: Empty state

    private var emptyState: some View {
        VStack(spacing: 20) {
            Image(systemName: "network")
                .font(.system(size: 64))
                .foregroundStyle(.secondary)
            Text("Tap **Run Scenarios** to execute\nall HttpMocker demo scenarios.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
        }
        .padding()
    }

    // MARK: Results list

    private var resultList: some View {
        List(viewModel.results) { item in
            NavigationLink(destination: ResultDetailView(item: item)) {
                ResultRowView(item: item)
            }
        }
        .listStyle(.insetGrouped)
        .overlay(alignment: .bottom) {
            if viewModel.isRunning {
                ProgressView("Running scenarios…")
                    .padding()
                    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
                    .padding(.bottom, 20)
            }
        }
    }

    // MARK: Run button

    private var runButton: some View {
        Button {
            viewModel.runScenarios()
        } label: {
            Label("Run Scenarios", systemImage: "play.fill")
        }
        .disabled(viewModel.isRunning)
    }
}

// MARK: – Result row

struct ResultRowView: View {
    let item: DemoViewModel.ResultItem

    var body: some View {
        HStack(spacing: 12) {
            Text(item.statusText)
                .font(.system(.body, design: .monospaced).bold())
                .foregroundStyle(.white)
                .frame(width: 52, height: 36)
                .background(item.statusColor, in: RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 2) {
                Text(item.scenario)
                    .font(.subheadline.weight(.medium))
                    .lineLimit(2)
                Text("\(item.method)  \(item.url)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: – Result detail

struct ResultDetailView: View {
    let item: DemoViewModel.ResultItem

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                infoCard
                bodyCard
                if item.isError {
                    errorCard
                }
            }
            .padding()
        }
        .navigationTitle(item.scenario)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var infoCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("Request", systemImage: "arrow.up.circle")
                .font(.headline)
            Divider()
            row("Method", item.method)
            row("URL", item.url)
            row("Status", item.statusText)
        }
        .padding()
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var bodyCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("Response Body", systemImage: "doc.text")
                .font(.headline)
            Divider()
            if item.body.isEmpty {
                Text("(empty)")
                    .foregroundStyle(.secondary)
                    .italic()
            } else {
                Text(item.body)
                    .font(.system(.body, design: .monospaced))
                    .textSelection(.enabled)
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private var errorCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label("Error", systemImage: "exclamationmark.triangle")
                .font(.headline)
                .foregroundStyle(.red)
            Divider()
            Text(item.error)
                .font(.system(.body, design: .monospaced))
                .foregroundStyle(.red)
                .textSelection(.enabled)
        }
        .padding()
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack(alignment: .top) {
            Text(label)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .frame(width: 70, alignment: .leading)
            Text(value)
                .font(.subheadline.weight(.medium))
                .textSelection(.enabled)
            Spacer()
        }
    }
}

#Preview {
    NavigationStack {
        ScenarioTestsView()
    }
}
