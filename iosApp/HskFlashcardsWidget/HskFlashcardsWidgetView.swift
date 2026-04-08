import SwiftUI
import WidgetKit
import AppIntents
import crossPlatform

struct HskFlashcardsWidgetView: View {
    var entry: HskEntry

    @Environment(\.widgetFamily) var family

    var body: some View {
        if !entry.isEmpty {
            VStack(spacing: 4) {
                // Top Row: Reload - Level - Speak
                HStack {
                    Button(intent: NextWordIntent()) {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 12))
                            .frame(width: 24, height: 24)
                            .background(Color.secondary.opacity(0.1))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)

                    Spacer()

                    if !entry.level.isEmpty {
                        Text(entry.level)
                            .font(.system(size: 10, weight: .medium))
                            .padding(.horizontal, 4)
                            .padding(.vertical, 2)
                            .background(Color.secondary.opacity(0.1))
                            .cornerRadius(4)
                    }

                    Spacer()

                    Button(intent: SpeakWordIntent(word: entry.word)) {
                        Image(systemName: "speaker.wave.2")
                            .font(.system(size: 12))
                            .frame(width: 24, height: 24)
                            .background(Color.secondary.opacity(0.1))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                }

                // Middle Content: Pinyin - Simplified - Definition
                Link(destination: URL(string: "hskwidget://search?q=\((crossPlatform.SearchQuery(query: entry.word, ignoreAnnotation: true, inListName: nil).description()).addingPercentEncoding(withAllowedCharacters: CharacterSet.urlQueryAllowed) ?? "")")!) {
                    VStack(spacing: 2) {
                        Spacer(minLength: 0)
                        
                        Text(entry.pinyin)
                            .font(.system(size: 14))
                            .foregroundColor(.secondary)
                            .lineLimit(1)

                        Text(entry.word)
                            .font(.system(size: 30, weight: .bold))
                            .minimumScaleFactor(0.5)
                            .lineLimit(1)

                        Text(entry.definition)
                            .font(.system(size: 13))
                            .lineLimit(2)
                            .multilineTextAlignment(.center)
                            .foregroundColor(.primary.opacity(0.8))
                        
                        Spacer(minLength: 0)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
        } else {
            Link(destination: URL(string: "hskwidget://configure")!) {
                VStack(spacing: 8) {
                    Spacer()
                    Image("AppIconSmall")
                        .resizable()
                        .frame(width: 60, height: 60)
                        .cornerRadius(12)

                    Text(crossPlatform.CachedResources.shared.widgetNotConfigured)
                        .font(.system(size: 14, weight: .medium))
                        .multilineTextAlignment(.center)
                        .foregroundColor(.secondary)
                    Spacer()
                }
            }
        }
    }
}
