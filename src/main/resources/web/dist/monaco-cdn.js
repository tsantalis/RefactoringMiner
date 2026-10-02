const MONACO_VS_URL = 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.39.0/min/vs';

function pinCodiconFont() {
    if (document.getElementById('codicon-font-face')) return;
    const style = document.createElement('style');
    style.id = 'codicon-font-face';
    style.textContent = `@font-face{font-family:codicon;font-display:block;src:url("${MONACO_VS_URL}/base/browser/ui/codicons/codicon/codicon.ttf") format("truetype")}`;
    document.head.appendChild(style);
}
