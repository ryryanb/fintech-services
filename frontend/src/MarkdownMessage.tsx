import type { ReactNode } from 'react';

function renderInline(text: string): ReactNode[] {
  const pattern = /(`[^`]+`|\*\*[^*]+\*\*|__[^_]+__|\*[^*]+\*|_[^_]+_|\[[^\]]+\]\([^)]+\))/g;
  return text.split(pattern).filter(Boolean).map((part, index) => {
    if (part.startsWith('`') && part.endsWith('`')) {
      return <code className="assistant-markdown-inline-code" key={index}>{part.slice(1, -1)}</code>;
    }
    if ((part.startsWith('**') && part.endsWith('**')) || (part.startsWith('__') && part.endsWith('__'))) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }
    if ((part.startsWith('*') && part.endsWith('*')) || (part.startsWith('_') && part.endsWith('_'))) {
      return <em key={index}>{part.slice(1, -1)}</em>;
    }
    const link = part.match(/^\[([^\]]+)\]\(([^)]+)\)$/);
    if (link) {
      const [, label, href] = link;
      const safeHref = /^(https?:\/\/|mailto:)/i.test(href) ? href : undefined;
      return safeHref
        ? <a href={safeHref} key={index} target="_blank" rel="noopener noreferrer">{label}</a>
        : <span key={index}>{label}</span>;
    }
    return <span key={index}>{part}</span>;
  });
}

function splitTableRow(line: string): string[] {
  return line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(cell => cell.trim());
}

function isBlockStart(line: string): boolean {
  return /^\s*(#{1,6}\s|\`\`\`|>\s?|[-*_]{3,}\s*$|[-*+]\s|\d+[.)]\s)/.test(line);
}

function renderBlocks(source: string, keyPrefix = 'block'): ReactNode[] {
  const lines = source.replace(/\r\n?/g, '\n').split('\n');
  const nodes: ReactNode[] = [];
  let i = 0;
  let key = 0;

  while (i < lines.length) {
    const line = lines[i];
    if (!line.trim()) { i += 1; continue; }

    if (/^\s*\`\`\`/.test(line)) {
      const language = line.trim().slice(3).trim();
      i += 1;
      const code: string[] = [];
      while (i < lines.length && !/^\s*\`\`\`/.test(lines[i])) code.push(lines[i++]);
      if (i < lines.length) i += 1;
      nodes.push(<pre className="assistant-markdown-code" key={key++}><code data-language={language || undefined}>{code.join('\n')}</code></pre>);
      continue;
    }

    const heading = line.match(/^(#{1,6})\s+(.+)$/);
    if (heading) {
      const level = heading[1].length;
      const content = renderInline(heading[2]);
      const props = { className: 'assistant-markdown-heading', key: key++ };
      if (level === 1) nodes.push(<h1 {...props}>{content}</h1>);
      else if (level === 2) nodes.push(<h2 {...props}>{content}</h2>);
      else if (level === 3) nodes.push(<h3 {...props}>{content}</h3>);
      else if (level === 4) nodes.push(<h4 {...props}>{content}</h4>);
      else if (level === 5) nodes.push(<h5 {...props}>{content}</h5>);
      else nodes.push(<h6 {...props}>{content}</h6>);
      i += 1;
      continue;
    }

    if (/^\s*([-*_])\1{2,}\s*$/.test(line)) {
      nodes.push(<hr key={key++} />);
      i += 1;
      continue;
    }

    if (line.includes('|') && i + 1 < lines.length && /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(lines[i + 1])) {
      const headers = splitTableRow(line);
      i += 2;
      const rows: string[][] = [];
      while (i < lines.length && lines[i].includes('|') && lines[i].trim()) rows.push(splitTableRow(lines[i++]));
      nodes.push(<div className="assistant-markdown-table-wrap" key={key++}><table className="assistant-markdown-table"><thead><tr>{headers.map((cell, cellIndex) => <th key={cellIndex}>{renderInline(cell)}</th>)}</tr></thead><tbody>{rows.map((row, rowIndex) => <tr key={rowIndex}>{headers.map((_, cellIndex) => <td key={cellIndex}>{renderInline(row[cellIndex] ?? '')}</td>)}</tr>)}</tbody></table></div>);
      continue;
    }

    if (/^>\s?/.test(line)) {
      const quote: string[] = [];
      while (i < lines.length && /^>\s?/.test(lines[i])) quote.push(lines[i++].replace(/^>\s?/, ''));
      nodes.push(<blockquote className="assistant-markdown-quote" key={key++}>{renderBlocks(quote.join('\n'), keyPrefix + '-quote')}</blockquote>);
      continue;
    }

    if (/^\s*[-*+]\s+/.test(line) || /^\s*\d+[.)]\s+/.test(line)) {
      const ordered = /^\s*\d+[.)]\s+/.test(line);
      const items: string[] = [];
      const matcher = ordered ? /^\s*\d+[.)]\s+/ : /^\s*[-*+]\s+/;
      while (i < lines.length && matcher.test(lines[i])) items.push(lines[i++].replace(matcher, ''));
      const List = ordered ? 'ol' : 'ul';
      nodes.push(<List className="assistant-markdown-list" key={key++}>{items.map((item, itemIndex) => <li key={itemIndex}>{renderInline(item)}</li>)}</List>);
      continue;
    }

    const paragraph = [line];
    i += 1;
    while (i < lines.length && lines[i].trim() && !isBlockStart(lines[i])) paragraph.push(lines[i++]);
    nodes.push(<p className="assistant-markdown-paragraph" key={key++}>{paragraph.map((part, partIndex) => <span key={partIndex}>{partIndex > 0 && <br />}{renderInline(part)}</span>)}</p>);
  }

  return nodes;
}

export default function MarkdownMessage({ content }: { content: string }) {
  return <div className="assistant-markdown">{renderBlocks(content)}</div>;
}
