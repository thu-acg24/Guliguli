const fs = require('fs')
const process = require('process')

export function getAppPath(argv: string[]): string {
    let p = argv
        .map(arg => {
            if (arg.includes('--app-path')) return arg.substring(11)
            return ''
        })
        .join('')
    if (p.endsWith('resources\\app')) {
        p = p.substring(0, p.length - 14)
        p = p.substring(0, p.lastIndexOf('\\'))
    }
    return p
}

export function readConfig() {
    const outcome = {} as any
    try {
        outcome.protocol = isHttps ? 'https' : 'http'
    } catch (error) {
        alert('alerting:' + error)
    }
    return outcome
}

const isHttps = window.location.protocol === 'https:'

export const config = readConfig()
