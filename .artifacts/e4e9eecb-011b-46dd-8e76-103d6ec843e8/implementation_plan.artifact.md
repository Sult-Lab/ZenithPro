# Branch Selector for Dashboard

This plan adds a persistent branch selector to the Dashboard screen, exclusive to Admin users. Admins can filter dashboard data by a specific branch or view an aggregate across all branches.

## User Review Required

> [!IMPORTANT]
> The branch selector will be implemented as a **Modal Bottom Sheet** as requested, triggered by a bar at the top of the dashboard. This differs slightly from the Dropdown menu in the provided guide to better suit the mobile "bottom sheet" pattern.

> [!NOTE]
> For Staff/Managers, the branch selector will be hidden, and they will continue to see data for their assigned branch only.

## Proposed Changes

### Core / Session Management
Modify `SessionManager` to handle branch selection persistence and provide a way for view models to react to branch changes.

#### [MODIFY] [SessionManager.kt](file:///Users/mac/AndroidStudioProjects/ZenithPro/app/src/main/java/com/techsultan/zenithpro/core/manager/SessionManager.kt)
- Update `setActiveBranch` to be a `suspend` function that persists the selection to `SessionDataStore`.
- Ensure `activeBranchId` and `activeBranchName` flows are correctly updated.

#### [MODIFY] [DashboardViewModel.kt](file:///Users/mac/AndroidStudioProjects/ZenithPro/app/src/main/java/com/techsultan/zenithpro/features/dashboard/presentation/DashboardViewModel.kt)
- Add `isAdmin`, `branches`, `activeBranchId`, and `activeBranchName` to `DashboardUiState`.
- Inject `BranchDao` to fetch available branches for the Admin.
- Observe `sessionManager.activeBranchId` and trigger data reload (`loadAll`) whenever the active branch changes.
- Implement `onBranchSelected` to update the global session.

### Dashboard UI
Implement the branch selector components and integrate them into the `DashboardScreen`.

#### [NEW] [BranchSelector.kt](file:///Users/mac/AndroidStudioProjects/ZenithPro/app/src/main/java/com/techsultan/zenithpro/features/dashboard/presentation/components/BranchSelector.kt)
- Create `BranchSelectorBar`: A clickable bar showing the current active branch.
- Create `BranchSelectorBottomSheet`: A `ModalBottomSheet` containing the list of branches and an "All branches" option.

#### [MODIFY] [DashboardScreen.kt](file:///Users/mac/AndroidStudioProjects/ZenithPro/app/src/main/java/com/techsultan/zenithpro/features/dashboard/presentation/DashboardScreen.kt)
- Integrate `BranchSelectorBar` into the `LazyColumn` (at the top, before the hero section).
- Add state to show/hide the `BranchSelectorBottomSheet`.

## Verification Plan

### Automated Tests
- N/A (UI focused change, manual verification preferred for layout/behavior).

### Manual Verification
1. **Admin User**:
   - Verify that the branch selector bar appears at the top of the dashboard.
   - Click the bar and verify the bottom sheet opens.
   - Select a branch; verify the dashboard data reloads and the bar text updates.
   - Select "All branches"; verify data reloads.
   - Restart the app and verify the selected branch is remembered.
2. **Staff User**:
   - Verify that the branch selector bar is **NOT** visible.
   - Verify data is correctly filtered for their assigned branch.
3. **Single Branch Business**:
   - Verify the selector bar is hidden if the business only has one branch.
