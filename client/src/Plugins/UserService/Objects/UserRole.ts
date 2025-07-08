export enum UserRole {
    admin = 'Admin',
    auditor = 'Auditor',
    normal = 'Normal'
}

export const userRoleList = Object.values(UserRole)

export function getUserRole(newType: string): UserRole {
    return userRoleList.filter(t => t === newType)[0]
}
