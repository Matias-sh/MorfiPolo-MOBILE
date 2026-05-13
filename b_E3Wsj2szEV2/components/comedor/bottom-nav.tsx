"use client"

import { motion, AnimatePresence } from "framer-motion"
import { CalendarDays, CalendarRange, User, Sparkles } from "lucide-react"
import { cn } from "@/lib/utils"

interface BottomNavProps {
  activeTab: "daily" | "weekly" | "profile"
  onTabChange: (tab: "daily" | "weekly" | "profile") => void
}

export function BottomNav({ activeTab, onTabChange }: BottomNavProps) {
  const tabs = [
    { id: "daily" as const, label: "Menu del Dia", icon: CalendarDays },
    { id: "weekly" as const, label: "Semanal", icon: CalendarRange },
    { id: "profile" as const, label: "Perfil", icon: User },
  ]

  return (
    <nav className="absolute bottom-0 left-0 right-0 z-40">
      {/* Glass background */}
      <div className="glass mx-3 mb-3 rounded-3xl shadow-premium-lg">
        <div className="flex items-center justify-around px-2 py-2">
          {tabs.map((tab) => {
            const Icon = tab.icon
            const isActive = activeTab === tab.id
            
            return (
              <motion.button
                key={tab.id}
                onClick={() => onTabChange(tab.id)}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className={cn(
                  "relative flex flex-col items-center gap-1 rounded-2xl px-4 py-2 transition-all duration-300",
                  isActive 
                    ? "text-primary" 
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                {/* Active background */}
                <AnimatePresence>
                  {isActive && (
                    <motion.div
                      className="absolute inset-0 rounded-2xl bg-gradient-to-br from-primary/15 to-accent/10"
                      layoutId="activeTab"
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.8 }}
                      transition={{ type: "spring", stiffness: 300, damping: 25 }}
                    />
                  )}
                </AnimatePresence>
                
                {/* Icon container */}
                <motion.div
                  className={cn(
                    "relative flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-300",
                    isActive 
                      ? "bg-gradient-to-br from-primary to-accent text-white shadow-lg shadow-primary/30" 
                      : "bg-transparent"
                  )}
                  animate={isActive ? { y: -2 } : { y: 0 }}
                >
                  <Icon className="h-5 w-5" strokeWidth={isActive ? 2.5 : 2} />
                  
                  {/* Active sparkle */}
                  {isActive && (
                    <motion.div
                      className="absolute -right-1 -top-1"
                      initial={{ scale: 0, rotate: -45 }}
                      animate={{ scale: 1, rotate: 0 }}
                      transition={{ type: "spring", delay: 0.1 }}
                    >
                      <Sparkles className="h-3 w-3 text-warning" />
                    </motion.div>
                  )}
                </motion.div>
                
                {/* Label */}
                <motion.span
                  className={cn(
                    "relative text-[10px] font-semibold tracking-wide transition-all",
                    isActive ? "text-primary" : "text-muted-foreground"
                  )}
                  animate={isActive ? { scale: 1.05 } : { scale: 1 }}
                >
                  {tab.label}
                </motion.span>

                {/* Active indicator dot */}
                <AnimatePresence>
                  {isActive && (
                    <motion.div
                      className="absolute -bottom-0.5 h-1 w-1 rounded-full bg-primary"
                      initial={{ scale: 0 }}
                      animate={{ scale: 1 }}
                      exit={{ scale: 0 }}
                      transition={{ type: "spring" }}
                    />
                  )}
                </AnimatePresence>
              </motion.button>
            )
          })}
        </div>
      </div>
    </nav>
  )
}
