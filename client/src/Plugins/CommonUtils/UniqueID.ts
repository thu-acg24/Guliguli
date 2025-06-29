import { create } from 'zustand'
import { randomString } from 'Plugins/CommonUtils/Functions/StringUtils'
import { persist, createJSONStorage } from 'zustand/middleware'
import { encryptionSessionStorage } from 'Plugins/CommonUtils/Functions/DefaultStorage'

const uniqueIDStore = create(
    persist(
        () => ({
            uniqueID: randomString(15),
        }),
        {
            name: 'uniqueIDStore',
            storage: createJSONStorage(() => encryptionSessionStorage),
        }
    )
)

export function getUniqueIDSnap(): string {
    return uniqueIDStore.getState().uniqueID
}
