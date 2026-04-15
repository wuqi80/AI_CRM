function normalizeAiText(value: string | undefined | null): string {
  return String(value || '')
    .replace(/\r\n?/g, '\n')
    .replace(/[ \t\f\v]+/g, ' ')
    .replace(/[ \t]+\n/g, '\n')
    .replace(/\n[ \t]+/g, '\n')
    .trim()
}

function truncateText(value: string, maxLength: number): string {
  if (value.length <= maxLength) return value
  return `${value.slice(0, Math.max(1, maxLength - 1)).trim()}…`
}

function stripLeadingMarker(value: string): string {
  return value.replace(/^[\s•·\-*]+/, '').replace(/^\d+[.)、．]\s*/, '').trim()
}

function splitInsightSegments(value: string): string[] {
  const normalized = normalizeAiText(value)
  if (!normalized) return []

  return normalized
    .split(/\n+|[；;。！？!?]+/g)
    .flatMap(segment => {
      const trimmed = stripLeadingMarker(segment)
      if (!trimmed) return []

      const commaParts = trimmed
        .split(/[，,、]/g)
        .map(part => stripLeadingMarker(part))
        .filter(Boolean)

      return commaParts.length > 1 ? commaParts : [trimmed]
    })
}

function shortenInsightSegment(value: string, maxLength: number): string {
  const cleaned = stripLeadingMarker(normalizeAiText(value))
  if (!cleaned) return ''

  const colonMatch = cleaned.match(/^([^:：]{1,12})[:：]\s*(.+)$/)
  if (colonMatch) {
    const [, label, detail] = colonMatch
    const detailFirstSegment = splitInsightSegments(detail)[0] || detail.trim()
    const detailMaxLength = Math.max(6, maxLength - label.length - 1)
    return truncateText(`${label} ${truncateText(detailFirstSegment, detailMaxLength)}`.trim(), maxLength)
  }

  return truncateText(cleaned, maxLength)
}

export function getCompactAiInsightItems(
  value: string | string[] | undefined | null,
  options: { maxItems?: number, maxLength?: number } = {}
): string[] {
  const maxItems = options.maxItems ?? 4
  const maxLength = options.maxLength ?? 16
  const sourceValues = Array.isArray(value) ? value : [value]
  const items: string[] = []

  for (const sourceValue of sourceValues) {
    for (const segment of splitInsightSegments(String(sourceValue || ''))) {
      const shortened = shortenInsightSegment(segment, maxLength)
      if (!shortened || items.includes(shortened)) continue
      items.push(shortened)
      if (items.length >= maxItems) return items
    }
  }

  return items
}

export function splitAiTextToParagraphs(
  value: string | undefined | null,
  options: { maxSentencesPerParagraph?: number } = {}
): string[] {
  const normalized = normalizeAiText(value)
  if (!normalized) return []

  const explicitParagraphs = normalized
    .split(/\n+/g)
    .map(paragraph => paragraph.trim())
    .filter(Boolean)

  if (explicitParagraphs.length > 1) {
    return explicitParagraphs
  }

  const sentences = normalized
    .split(/(?<=[。！？!?；;])/g)
    .map(sentence => sentence.trim())
    .filter(Boolean)

  if (sentences.length <= 1) {
    return [normalized]
  }

  const maxSentencesPerParagraph = options.maxSentencesPerParagraph ?? 2
  const paragraphs: string[] = []

  for (let index = 0; index < sentences.length; index += maxSentencesPerParagraph) {
    paragraphs.push(sentences.slice(index, index + maxSentencesPerParagraph).join(' '))
  }

  return paragraphs
}
