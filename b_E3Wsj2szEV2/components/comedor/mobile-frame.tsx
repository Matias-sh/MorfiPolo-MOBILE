"use client"

import { type ReactNode, useState, useEffect } from "react"
import { motion } from "framer-motion"
import { Signal, Wifi, Battery } from "lucide-react"

interface MobileFrameProps {
  children: ReactNode
}

export function MobileFrame({ children }: MobileFrameProps) {
  const [time, setTime] = useState("11:01")

  useEffect(() => {
    const updateTime = () => {
      const now = new Date()
      setTime(now.toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit", hour12: false }))
    }
    updateTime()
    const interval = setInterval(updateTime, 1000)
    return () => clearInterval(interval)
  }, [])

  return (
    <motion.div 
      className="relative mx-auto h-[812px] w-[375px]"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      {/* Outer frame with premium gradient border */}
      <div className="absolute inset-0 rounded-[52px] bg-gradient-to-b from-zinc-700 via-zinc-800 to-zinc-900 p-[3px] shadow-2xl">
        {/* Inner bezel */}
        <div className="h-full w-full overflow-hidden rounded-[49px] bg-gradient-to-b from-zinc-800 to-zinc-900 p-2">
          {/* Screen container */}
          <div className="relative h-full w-full overflow-hidden rounded-[42px] bg-background shadow-inner">
            {/* Dynamic Island */}
            <motion.div 
              className="absolute left-1/2 top-3 z-50 -translate-x-1/2"
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: 0.3, type: "spring" }}
            >
              <div className="flex h-8 w-28 items-center justify-center rounded-full bg-zinc-900/95 shadow-lg">
                {/* Camera lens */}
                <div className="absolute left-5 h-3 w-3 rounded-full bg-zinc-800/80 ring-1 ring-zinc-700/50">
                  <div className="absolute inset-0.5 rounded-full bg-gradient-to-br from-zinc-600/50 to-transparent" />
                  <motion.div 
                    className="absolute inset-1 rounded-full bg-zinc-900"
                    animate={{ 
                      boxShadow: [
                        "inset 0 0 2px rgba(255,255,255,0.1)",
                        "inset 0 0 4px rgba(255,255,255,0.2)",
                        "inset 0 0 2px rgba(255,255,255,0.1)"
                      ]
                    }}
                    transition={{ duration: 3, repeat: Infinity }}
                  />
                </div>
              </div>
            </motion.div>

            {/* Status Bar */}
            <motion.div 
              className="absolute left-0 right-0 top-0 z-40 flex h-12 items-center justify-between px-8 pt-1"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.5 }}
            >
              {/* Time */}
              <motion.span 
                className="text-sm font-semibold text-foreground"
                key={time}
                initial={{ opacity: 0.5 }}
                animate={{ opacity: 1 }}
              >
                {time}
              </motion.span>
              
              {/* Status icons */}
              <div className="flex items-center gap-1.5">
                <Signal className="h-4 w-4 text-foreground" strokeWidth={2} />
                <Wifi className="h-4 w-4 text-foreground" strokeWidth={2} />
                <div className="relative flex items-center">
                  <div className="relative h-3 w-6 rounded-sm border-[1.5px] border-foreground">
                    <motion.div 
                      className="absolute inset-0.5 rounded-[2px] bg-success"
                      initial={{ width: "70%" }}
                      animate={{ width: ["70%", "65%", "70%"] }}
                      transition={{ duration: 10, repeat: Infinity }}
                    />
                  </div>
                  <div className="ml-0.5 h-1.5 w-0.5 rounded-r-sm bg-foreground" />
                </div>
              </div>
            </motion.div>
            
            {/* Content */}
            <div className="h-full overflow-hidden pt-12">
              {children}
            </div>
            
            {/* Home Indicator */}
            <motion.div 
              className="absolute bottom-2 left-1/2 z-50 h-1 w-32 -translate-x-1/2 rounded-full bg-foreground/40"
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.6 }}
            />
          </div>
        </div>
      </div>

      {/* Side buttons */}
      {/* Power button */}
      <div className="absolute -right-[3px] top-32 h-12 w-1 rounded-r-sm bg-zinc-700" />
      
      {/* Volume buttons */}
      <div className="absolute -left-[3px] top-28 h-8 w-1 rounded-l-sm bg-zinc-700" />
      <div className="absolute -left-[3px] top-40 h-8 w-1 rounded-l-sm bg-zinc-700" />
      
      {/* Silent switch */}
      <div className="absolute -left-[3px] top-20 h-5 w-1 rounded-l-sm bg-zinc-700" />

      {/* Subtle reflection */}
      <div className="pointer-events-none absolute inset-0 rounded-[52px] bg-gradient-to-br from-white/5 via-transparent to-transparent" />
    </motion.div>
  )
}
