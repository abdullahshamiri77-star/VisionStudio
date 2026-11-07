
# VISION STUDIO
## Professional Android Video Editor

## 1. Project Goal
Build a real Android video editor inspired by professional
editing workflows, with independent identity and architecture.

## 2. Development Rules
- Preserve existing working features.
- Never replace complete files without reviewing their contents.
- Make incremental changes.
- Create a Git commit before major modifications.
- Build and test after meaningful changes.
- Never mark a feature complete based on UI appearance alone.
- Test actual functionality on Android.
- Document every accepted feature and known limitation.

## 3. Architecture

### Foundation
- Project Manager
- Project Repository
- Storage Manager
- Project State
- Undo / Redo
- Error Handling

### Core Engines
- Timeline Engine
- Media Engine
- Playback Engine
- Effects Engine
- Audio Engine
- Color Engine
- Render Engine

### Workspaces
- Media
- Cut
- Edit
- Fusion
- Color
- Fairlight
- Deliver

## 4. Development Phases

Phase 0: Audit and preserve current project
Phase 1: Timeline Core
Phase 2: Edit Workspace
Phase 3: Playback Engine
Phase 4: Render Engine
Phase 5: Effects and Transitions
Phase 6: Color Engine
Phase 7: Audio Engine
Phase 8: Fusion Workspace
Phase 9: Testing and Optimization

## 5. Feature Acceptance
A feature is complete only when:
1. Its code is implemented.
2. The project builds successfully.
3. The feature works on the Android device.
4. Existing features remain functional.
5. Its limitations are documented.

## 6. Current Status
- Android project exists.
- GitHub Actions debug APK build works.
- Video import and playback exist.
- Basic timeline UI exists.
- Timeline Models and Engine files exist.
- Professional multi-track editing is not yet verified.
- Advanced Color and Fusion are not implemented.

## 7. Immediate Next Task
Audit MainActivity.kt and existing timeline files.
Preserve current functionality.
Implement Timeline Core V1 incrementally.

## 8. Update Log
Record every accepted feature, test result,
build reference, and known issue here.
