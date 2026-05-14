## Outlook-style `.eml` thread generator

Generates an Outlook-friendly `.eml` with a quoted mail thread from a JSON file.

### Requirements

- Windows + Node.js

### Generate

```powershell
node C:\Users\Admin\OneDrive\Documents\New project\tools\outlook-eml\generate-eml.mjs `
  --input C:\Users\Admin\OneDrive\Documents\New project\tools\outlook-eml\thread.example.json `
  --output C:\Users\Admin\OneDrive\Documents\New project\tools\outlook-eml\out.eml
```

### Timezone

- Set `timezone_offset_minutes` (example: `330` for `+05:30`)
- Use ISO datetimes with offsets for `date` and quoted messages `sent_iso` (example: `2026-03-30T12:51:00+05:30`)

