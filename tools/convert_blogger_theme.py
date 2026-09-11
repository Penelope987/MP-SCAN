#!/usr/bin/env python3
"""Convert the MP SCAN Blogger XHTML theme into an APK-local HTML document."""
from pathlib import Path
import re
import sys

source = Path(sys.argv[1])
target = Path(sys.argv[2])
text = source.read_text(encoding="utf-8")

text = re.sub(r"^<\?xml[^>]*>\s*", "", text)
text = re.sub(r"<html\b[^>]*>", "<html lang='pt-BR'>", text, count=1)
text = text.replace("<b:skin><![CDATA[", "<style id='mp-theme-style'>")
text = text.replace("]]></b:skin>", "</style>")
text = re.sub(r"\s*<!-- Blogger exige pelo menos uma seção.*?</b:section>\s*", "\n", text, flags=re.S)

# XHTML allows `<div/>`; HTML does not. Expand non-void self-closing elements.
void = {"area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr"}
pattern = re.compile(r"<([A-Za-z][\w:-]*)(\s[^<>]*?)?\s*/>")
def expand(match):
    tag = match.group(1)
    attrs = match.group(2) or ""
    return match.group(0) if tag.lower() in void else f"<{tag}{attrs}></{tag}>"
text = pattern.sub(expand, text)

# Remove Blogger-only remnants if a future theme adds them outside the hidden section.
text = re.sub(r"<b:[^>]+>.*?</b:[^>]+>", "", text, flags=re.S)
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(text, encoding="utf-8")
print(f"created {target} ({target.stat().st_size} bytes)")
