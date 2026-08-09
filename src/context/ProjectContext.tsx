import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import * as projectsApi from '../api/projects';
import { WorkflowState } from '../types';

const POLL_INTERVAL_MS = 20000;

interface ProjectContextValue {
  state: WorkflowState | null;
  setState: (s: WorkflowState | null) => void;
  refreshState: (locale?: string) => Promise<void>;
  hasProject: boolean;
  /** True once a newly-signed build appears that the user hasn't acknowledged yet. */
  newBuildAvailable: boolean;
  /** Server-relative path of the new signed APK, for direct download/install. */
  newBuildFile: string | null;
  /** Call after the user installs/dismisses the "new build available" banner. */
  dismissNewBuild: () => void;
}

const ProjectContext = createContext<ProjectContextValue | undefined>(undefined);

export function ProjectProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<WorkflowState | null>(null);
  const [newBuildAvailable, setNewBuildAvailable] = useState(false);
  const [newBuildFile, setNewBuildFile] = useState<string | null>(null);

  // Tracks the last signed_apk we've already surfaced to the user, so we only
  // announce genuinely *new* builds (not the one already on screen at mount).
  const lastSeenSignedApk = useRef<string | null | undefined>(undefined);

  const refreshState = useCallback(async (locale = 'values') => {
    const res = await projectsApi.getWorkflowState(locale);
    if (res.status !== 'success') return;
    const next: WorkflowState = res.state;

    const currentSigned = next?.signed_apk || null;
    if (lastSeenSignedApk.current === undefined) {
      // First load for this project — don't announce, just baseline it.
      lastSeenSignedApk.current = currentSigned;
    } else if (currentSigned && currentSigned !== lastSeenSignedApk.current) {
      lastSeenSignedApk.current = currentSigned;
      setNewBuildFile(currentSigned);
      setNewBuildAvailable(true);
    }

    setState(next);
  }, []);

  const dismissNewBuild = useCallback(() => {
    setNewBuildAvailable(false);
  }, []);

  const hasProject = !!state?.project_id;

  // Poll for new signed builds while a project is open, so a build kicked off
  // elsewhere (or finishing async on the server) surfaces here automatically.
  useEffect(() => {
    if (!hasProject) return;
    const id = setInterval(() => {
      refreshState().catch(() => {
        // transient network errors are fine to ignore for background polling
      });
    }, POLL_INTERVAL_MS);
    return () => clearInterval(id);
  }, [hasProject, refreshState]);

  const value = useMemo(
    () => ({
      state,
      setState,
      refreshState,
      hasProject,
      newBuildAvailable,
      newBuildFile,
      dismissNewBuild,
    }),
    [state, refreshState, hasProject, newBuildAvailable, newBuildFile, dismissNewBuild],
  );

  return <ProjectContext.Provider value={value}>{children}</ProjectContext.Provider>;
}

export function useProject() {
  const ctx = useContext(ProjectContext);
  if (!ctx) throw new Error('useProject must be used within ProjectProvider');
  return ctx;
}
