import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import { encryptionSessionStorage } from 'Plugins/CommonUtils/Functions/DefaultStorage'
import { generateKeyPair } from 'Plugins/CommonUtils/Encryption/EncryptionUtils'

const encryptionStore = create(
    persist(() => generateKeyPair(), {
        name: 'encryptionStore',
        storage: createJSONStorage(() => encryptionSessionStorage)
    })
)

export function getPrivateKeySnap(): string {
    return encryptionStore.getState()?.clientPrivate
}

export function getPublicKeySnap(): string {
    return encryptionStore.getState()?.clientPublic
}
