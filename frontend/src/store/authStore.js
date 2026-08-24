import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export const useAuthStore = create(
  persist(
    (set) => ({
      user: null,
      token: null,
      currentOrgId: null,
      login: (token, user) => set({ token, user }),
      logout: () => set({ token: null, user: null, currentOrgId: null }),
      setCurrentOrgId: (orgId) => set({ currentOrgId: orgId }),
    }),
    { name: 'collabos-auth' },
  ),
)
