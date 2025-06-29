import { useEffect, useRef } from 'react'

/**
 * 当value变化时，返回value的旧值
 * @param value
 */
export const usePrevious = <T>(value: T): T | undefined => {
    const ref = useRef<T>(undefined)
    useEffect(() => {
        ref.current = value
    }, [value])
    return ref.current
}
