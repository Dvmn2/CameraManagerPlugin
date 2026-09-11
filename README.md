# CameraManagerPlugin

Paper plugin for Minecraft 1.21.11. Server-side counterpart to the CameraManager Fabric client mod. Sends camera effect
instructions to clients over a custom plugin-messaging channel.

This project is under active development. The only implemented feature at this time is camera shake.

## Requirements

- Paper 1.21.11 or a compatible fork
- Java 21
- CameraManager client mod installed on players who should receive camera effects

## Features

### Camera shake

Triggers a temporary camera shake effect on one or more players. The effect is applied entirely on the client; the
server only sends the parameters (angle offset, position offset, duration) and does not track playback state.

## Commands

All commands are under `/camera`.

| Command                                                                 | Description                                                                                                                                            |
|-------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/camera shake add [targets] [angle_delta] [position_delta] [duration]` | Starts a camera shake. If `targets` is omitted, applies to the command sender. Defaults: `angle_delta=20`, `position_delta=20`, `duration=20` (ticks). |
| `/camera shake stop [targets]`                                          | Stops any active camera shake. If `targets` is omitted, applies to the command sender.                                                                 |

## Permissions

| Permission                  | Description                     | Default |
|-----------------------------|---------------------------------|---------|
| `cameramanager.admin`       | Access to the `/camera` command | op      |
| `cameramanager.shake.admin` | Access to `/camera shake`       | op      |

## Configuration

`config.yml`:

```yaml
settings:
  enabled: true
  language: "auto"
```

`language` controls the language of command feedback messages. Accepted values:

- `ru` — Russian
- `en` — English
- `auto` — resolved per command sender from their client locale (console defaults to English)

## Installation

1. Place the built jar in the server `plugins` folder.
2. Start the server once to generate `config.yml`.
3. Adjust permissions and `config.yml` as needed.
4. Ensure players who should see camera effects have the CameraManager client mod installed.

## Networking

The plugin registers two outgoing plugin-messaging channels:

- `cameramanager:shake` — starts a shake (payload: three 32-bit integers — angle delta, position delta, duration in
  ticks)
- `cameramanager:shake_stop` — stops all active shakes (no payload)

These channel identifiers must match the `CustomPayload` identifiers registered by the CameraManager client mod.