#!/bin/bash
# ============================================
# OpenCode setup for Repertorio project
# Run once per machine after cloning/pulling.
# ============================================
set -euo pipefail

echo "==> Updating OMO-Slim bundled skills..."
npx oh-my-opencode-slim@latest install --skills=force

echo "==> Adding sub-agent-statusline TUI plugin..."
TUI_CONFIG="$HOME/.config/opencode/tui.json"
if [ -f "$TUI_CONFIG" ]; then
  # Add only if not already present
  if ! grep -q "opencode-subagent-statusline" "$TUI_CONFIG"; then
    # Use python to safely add to JSON array (portable, no jq dependency)
    python3 -c "
import json
with open('$TUI_CONFIG') as f:
    cfg = json.load(f)
cfg.setdefault('plugin', [])
if 'opencode-subagent-statusline' not in cfg['plugin']:
    cfg['plugin'].append('opencode-subagent-statusline')
with open('$TUI_CONFIG', 'w') as f:
    json.dump(cfg, f, indent=2)
    f.write('\n')
"
    echo "   Added to $TUI_CONFIG"
  else
    echo "   Already present, skipping"
  fi
else
  echo "   $TUI_CONFIG not found, creating..."
  echo '{"plugin": ["opencode-subagent-statusline"]}' > "$TUI_CONFIG"
fi

echo ""
echo "Done. Restart OpenCode to apply."
echo ""
echo "Verify with: opencode"
echo "Then type: ping all agents"
