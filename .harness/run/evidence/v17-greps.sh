#!/bin/bash
# The Verifier's greps and diffs (Iteration 17), one heading per criterion; "exit=N" after each command.
cd /d/Valve/Personal/Kanso || exit 2
B=a720960
x() { echo "exit=$?"; }
echo "## C8"; git grep -n -E '(LoadingIndicator|ContainedLoadingIndicator|CircularProgressIndicator|LinearProgressIndicator)\(' -- app/src/main/java/com/phongkaster/kanso/ui/home app/src/main/java/com/phongkaster/kanso/ui/myedits; x
echo "## C16 camera shapes"; git grep -n -E '\b(Button|OutlinedButton|FilledTonalButton|TextButton|IconButton|FilledIconButton|FilledTonalIconButton|IconToggleButton|ToggleButton)\(' -- app/src/main/java/com/phongkaster/kanso/ui/camera/CameraScreen.kt; x
echo "## C19 all button calls in app/src/main"; git grep -n -E '\b(Button|OutlinedButton|FilledTonalButton|TextButton|IconButton|FilledIconButton|FilledTonalIconButton|IconToggleButton|ToggleButton)\(' -- app/src/main; x
echo "## C19 shapes = lines"; git grep -n -c 'shapes =' -- app/src/main; x
echo "## C19 MaterialExpressiveTheme"; git grep -n 'MaterialExpressiveTheme(' -- app/src/main; x
echo "## C20a-1"; git grep -n -E 'ObjectAnimator|ValueAnimator|AnimatorSet|android\.view\.animation|ViewPropertyAnimator|\.animate\(\)' -- app/src/main; x
echo "## C20a-2"; git grep -n -i -E 'lottie|airbnb|shimmer' -- app/src/main gradle/libs.versions.toml app/build.gradle.kts; x
echo "## C20a-2 at base"; git grep -n -i -E 'lottie|airbnb|shimmer' $B -- gradle/libs.versions.toml app/build.gradle.kts; x
echo "## C23 fixed colours"; git grep -n -E 'Color\(0x|Color\.(Black|White)' -- app/src/main; x
echo "## C23 colors.xml"; git diff --exit-code $B HEAD -- app/src/main/res/values/colors.xml app/src/main/res/values-night/colors.xml; x
echo "## C23 bitmaps"; git diff --name-only --diff-filter=A $B HEAD -- app/src/main/res | grep -i -E '\.(png|jpe?g|webp|gif)$'; x
echo "## C33"; diff <(git grep -n -E '"theme_mode"|"theme_colour_source"|"theme_colour_seed"|settings\.preferences_pb' HEAD -- app/src/main | sed 's/^HEAD://') <(git grep -n -E '"theme_mode"|"theme_colour_source"|"theme_colour_seed"|settings\.preferences_pb' $B -- app/src/main | sed "s/^$B://"); x
echo "## C35 assume"; git grep -n -E '@Ignore|assumeTrue|assumeFalse|Assume\.' HEAD -- app/src/test; x
echo "## C35 @Test lines"; git grep -h -I -E '^\s*@Test' HEAD -- app/src/test/java | wc -l
echo "## C36 MaterialTheme"; git grep -n -E 'MaterialTheme\(' -- app/src/main; x
git grep -n 'androidx.compose.material\.' -- app/src/main; x
echo "## C36 guard lists"; git grep -n -E 'emptySet\(\)|ALLOWED' -- app/src/test/java/com/phongkaster/kanso/conventions/OneTypePerFileTest.kt app/src/test/java/com/phongkaster/kanso/*/OneTypePerFileTest.kt 2>/dev/null; x
git diff $B HEAD -- 'app/src/test/**/PreviewCoverageTest.kt' 'app/src/test/**/PreviewHygieneTest.kt' 'app/src/test/**/OneTypePerFileTest.kt' | head -80
echo "## C36 ScreenStateTest diff"; git diff $B HEAD -- 'app/src/test/**/ScreenStateTest.kt' | head -60
echo "## C36 requiredPreviewNames diff"; git diff $B HEAD -- 'app/src/test/**/PreviewsRenderTest.kt' | head -120
echo "## C37"
grep -n -i -c 'carousel' app/src/main/java/com/phongkaster/kanso/ui/home/HomeScreen.kt
grep -n -E 'HorizontalUncontainedCarousel|HorizontalMultiBrowseCarousel|HorizontalCenteredHeroCarousel' app/src/main/java/com/phongkaster/kanso/ui/home/HomeScreen.kt | head -3
grep -n -i -E 'spinner' app/src/main/java/com/phongkaster/kanso/ui/home/HomeScreen.kt | head -3
grep -n -i -c 'tab' app/src/main/java/com/phongkaster/kanso/navigation/KansoTabBar.kt; grep -n -c 'inverseSurface' app/src/main/java/com/phongkaster/kanso/navigation/KansoTabBar.kt
grep -n -i -E 'back stack' app/src/main/java/com/phongkaster/kanso/navigation/KansoNavHost.kt | head -3
grep -n -E 'blur' app/src/main/java/com/phongkaster/kanso/ui/common/BlurredBackdrop.kt | head -2; grep -n 'Android 12' app/src/main/java/com/phongkaster/kanso/ui/common/BlurredBackdrop.kt | head -2
grep -n 'Brush' app/src/main/java/com/phongkaster/kanso/ui/saved/SavedScreen.kt | head -2; grep -n -i 'gradient' app/src/main/java/com/phongkaster/kanso/ui/saved/SavedScreen.kt | head -2
grep -n 'LazyVerticalGrid' app/src/main/java/com/phongkaster/kanso/ui/myedits/MyEditsScreen.kt | head -2
grep -n -i -E 'base 16|opaque' core/colorpicker/src/main/java/com/library/colorpicker/HSVColorPicker.kt
echo "## C38 suppress added"; git diff $B HEAD -- app/src/main | grep -E '^\+.*@Suppress\("DEPRECATION(_ERROR)?"\)'; x
echo "## C39 names"; git diff --name-only $B HEAD -- core opencv libs
echo "## C39 hunks"; git diff -U0 $B HEAD -- core/colorpicker/src/main/java/com/library/colorpicker/HSVColorPicker.kt | grep '^@@'
echo "## C40 build files"; git diff --exit-code $B HEAD -- gradle/libs.versions.toml settings.gradle.kts build.gradle.kts gradle.properties app/build.gradle.kts opencv libs; x
echo "## C40 reference"; git diff --quiet a0d9a79 HEAD -- reference; x
git grep -n 'reference/' -- settings.gradle.kts app; x
echo "## C40 drawables added"; git diff --name-only --diff-filter=A $B HEAD -- app/src/main/res/drawable/
for f in $(git diff --name-only --diff-filter=A $B HEAD -- app/src/main/res/drawable/); do n=$(basename $f .xml); head -c 200 $f | grep -c '<vector'; git grep -c "R.drawable.$n\b" -- app/src/main/java | head -1; grep -c "$n" .harness/run/evidence/symbols/SOURCES.md; done
echo "## C41"; ls docs/adr/; grep -n 'superseded in part\|Superseded in part' docs/adr/ADR-001-*.md; grep -c -E '2026-10-12|ADR-001' docs/adr/ADR-002-*.md
echo "## R1"; git grep -n -E 'HorizontalFloatingToolbar|IconToggleButton|FloatingToolbarDefaults|EditorToolbar|ToolToggle|DISABLED_ICON_ALPHA' -- app/src/main/java/com/phongkaster/kanso/ui/tool; x
git grep -n -E 'EDITOR_TOOLBAR_TAG|editorToolTag|editor-toolbar|editor-tool-' -- app/src; x
echo "## R2"; git grep -n -w -E 'selectTool|editingTools|keptToolName|onSelectTool' -- app/src; x
git grep -n -i -E 'switch(es|ing)? tools?|tools? (can )?switch|toolbar toggle' -- app/src/main/java/com/phongkaster/kanso/ui/tool app/src/main/java/com/phongkaster/kanso/navigation; x
echo "## R3"; git grep -n -w -E 'SettingsEntry|SETTINGS_MIN_HEIGHT|onSettingsClick' -- app/src; x
echo "## R4"; git grep -n -w -E 'OpenPhotoButton|OPEN_PHOTO_HEIGHT|HOME_OPEN_PHOTO_TAG|onOpenPhoto|home_open_photo|CameraButton|onCameraClick' -- app/src/main/java/com/phongkaster/kanso/ui/home app/src/test; x
git grep -n -E 'home-open-photo|Open a photo' -- app/src; x
ls app/src/main/res/drawable/ic_add_photo_alternate.xml; x
echo "## R5"; git grep -n -w -E 'ToolGroup|GroupHeader|ToolCard|CARD_MIN_HEIGHT|PRESSED_CARD_SCALE' -- app/src/main/java/com/phongkaster/kanso/ui/home; x
git grep -n -w -E 'aiTools|editTools|homeToolTag|home_header_ai_tools|home_header_edit' -- app/src; x
git grep -n 'home-tool-' -- app/src; x
echo "## R6"; git grep -n -w 'onBack' -- app/src/main/java/com/phongkaster/kanso/ui/settings; x
git grep -n -E 'R\.string\.back|ic_arrow_back' -- app/src/main/java/com/phongkaster/kanso/ui/settings; x
echo "## R7"; git grep -n -E 'SAVED_HOME_TAG|SAVED_HOME_ICON_TAG|SAVED_ACTIONS_TAG|saved-home|saved-actions' -- app/src; x
echo "## R8"; git ls-files app/src/test/java/com/phongkaster/kanso/tool/ToolSelectionAnimationTest.kt; echo "---orphans"; bash .harness/run/evidence/orphans.sh; x
echo "## R9 named"; for n in home_open_photo home_header_ai_tools home_header_edit saved_home ic_add_photo_alternate; do echo "$n res:$(git grep -l -w $n -- app/src/main/res | tr '\n' ' ') java:$(git grep -l -w -E "R\.(string|drawable)\.$n" -- app/src/main/java | tr '\n' ' ')"; done
