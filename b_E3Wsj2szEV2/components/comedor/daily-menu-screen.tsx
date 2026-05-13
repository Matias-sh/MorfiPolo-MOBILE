"use client"

import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { Clock, CheckCircle2, XCircle, ChefHat, Sparkles, UtensilsCrossed, Flame } from "lucide-react"
import { cn } from "@/lib/utils"

interface MenuOption {
  id: string
  name: string
  description?: string
  selected?: boolean
  calories?: string
}

interface DailyMenuScreenProps {
  date?: string
  timeRange?: string
  isOpen?: boolean
  hasSelected?: boolean
  options?: MenuOption[]
}

export function DailyMenuScreen({
  date = "28/04/2026",
  timeRange = "08:00 - 11:00",
  isOpen = true,
  hasSelected = true,
  options = [
    { id: "1", name: "Muslo deshuesado con papas, calabaza y cebollas", selected: true, calories: "520 kcal" },
    { id: "2", name: "Milanesa napolitana con pure de papas", selected: false, calories: "680 kcal" },
  ]
}: DailyMenuScreenProps) {
  const [selectedOption, setSelectedOption] = useState<string | null>(
    options.find(o => o.selected)?.id || null
  )
  const [showConfetti, setShowConfetti] = useState(false)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const handleSelect = (id: string) => {
    if (!isOpen || hasSelected) return
    setSelectedOption(id)
    setShowConfetti(true)
    setTimeout(() => setShowConfetti(false), 2000)
  }

  return (
    <div className="relative flex h-full flex-col overflow-hidden bg-background">
      {/* Confetti effect */}
      <AnimatePresence>
        {showConfetti && (
          <div className="pointer-events-none absolute inset-0 z-50 overflow-hidden">
            {[...Array(24)].map((_, i) => (
              <motion.div
                key={i}
                className="absolute"
                style={{
                  left: `${Math.random() * 100}%`,
                  top: "40%",
                }}
                initial={{ y: 0, opacity: 1, scale: 1 }}
                animate={{
                  y: [0, -250 - Math.random() * 150],
                  x: [(Math.random() - 0.5) * 150],
                  rotate: [0, 360 * (Math.random() > 0.5 ? 1 : -1) * 2],
                  opacity: [1, 1, 0],
                  scale: [1, 1.2, 0.8],
                }}
                exit={{ opacity: 0 }}
                transition={{ duration: 1.8, ease: "easeOut" }}
              >
                {i % 3 === 0 ? (
                  <div className="h-3 w-3 rounded-full bg-primary" />
                ) : i % 3 === 1 ? (
                  <div className="h-2 w-4 rounded-sm bg-accent" />
                ) : (
                  <Sparkles className="h-4 w-4 text-warning" />
                )}
              </motion.div>
            ))}
          </div>
        )}
      </AnimatePresence>

      {/* Hero Header */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={mounted ? { opacity: 1 } : {}}
        className="relative overflow-hidden"
      >
        {/* Animated Gradient Background */}
        <motion.div 
          className="absolute inset-0 bg-gradient-to-br from-primary via-primary/95 to-accent/80"
          animate={{
            backgroundPosition: ["0% 0%", "100% 100%", "0% 0%"],
          }}
          transition={{ duration: 10, repeat: Infinity, ease: "linear" }}
          style={{ backgroundSize: "200% 200%" }}
        />
        
        {/* Floating decorative elements */}
        <motion.div
          className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-white/10"
          animate={{ 
            scale: [1, 1.2, 1],
            rotate: [0, 90, 0]
          }}
          transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
        />
        <motion.div
          className="absolute -bottom-16 -left-8 h-40 w-40 rounded-full bg-white/5"
          animate={{ 
            scale: [1, 1.15, 1],
            x: [0, 10, 0]
          }}
          transition={{ duration: 6, repeat: Infinity, ease: "easeInOut", delay: 1 }}
        />
        
        {/* Floating food icons */}
        <motion.div
          className="absolute right-6 top-8"
          animate={{ y: [-5, 5, -5], rotate: [0, 10, 0] }}
          transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
        >
          <ChefHat className="h-6 w-6 text-white/30" />
        </motion.div>

        <div className="relative px-6 pb-10 pt-6">
          {/* Date display */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={mounted ? { opacity: 1, y: 0 } : {}}
            transition={{ delay: 0.2, duration: 0.6 }}
            className="text-center"
          >
            <motion.div
              className="mb-4 inline-flex items-center justify-center rounded-2xl bg-white/15 px-4 py-2 backdrop-blur-sm"
              whileHover={{ scale: 1.05 }}
            >
              <UtensilsCrossed className="mr-2 h-5 w-5 text-white/80" />
              <span className="text-sm font-medium text-white/90">Menu del Dia</span>
            </motion.div>
            
            <motion.h1 
              className="mb-2 text-5xl font-bold tracking-tight text-white"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={mounted ? { opacity: 1, scale: 1 } : {}}
              transition={{ delay: 0.3, type: "spring", stiffness: 200 }}
            >
              {date}
            </motion.h1>
            
            <motion.div 
              className="flex items-center justify-center gap-2 text-white/80"
              initial={{ opacity: 0 }}
              animate={mounted ? { opacity: 1 } : {}}
              transition={{ delay: 0.5 }}
            >
              <motion.div
                animate={{ rotate: [0, 360] }}
                transition={{ duration: 20, repeat: Infinity, ease: "linear" }}
              >
                <Clock className="h-4 w-4" />
              </motion.div>
              <span className="text-sm font-medium">
                Horario: {timeRange}
              </span>
            </motion.div>
          </motion.div>

          {/* Status badges */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={mounted ? { opacity: 1, y: 0 } : {}}
            transition={{ delay: 0.6 }}
            className="mt-5 flex items-center justify-center gap-3"
          >
            {hasSelected && (
              <motion.div
                initial={{ scale: 0, rotate: -180 }}
                animate={{ scale: 1, rotate: 0 }}
                transition={{ type: "spring", stiffness: 200, delay: 0.7 }}
                className="flex items-center gap-2 rounded-full bg-white px-4 py-2.5 shadow-lg shadow-black/10"
              >
                <motion.div
                  animate={{ scale: [1, 1.2, 1] }}
                  transition={{ duration: 1.5, repeat: Infinity }}
                >
                  <CheckCircle2 className="h-5 w-5 text-success" />
                </motion.div>
                <span className="text-sm font-bold text-success">Ya elegiste</span>
              </motion.div>
            )}
            
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: "spring", stiffness: 200, delay: 0.8 }}
              className={cn(
                "flex items-center gap-2 rounded-full px-4 py-2.5 shadow-lg",
                isOpen 
                  ? "bg-white/20 backdrop-blur-md" 
                  : "bg-destructive/90"
              )}
            >
              {isOpen ? (
                <motion.div
                  className="h-2.5 w-2.5 rounded-full bg-success"
                  animate={{ 
                    scale: [1, 1.4, 1],
                    opacity: [1, 0.7, 1]
                  }}
                  transition={{ duration: 1.5, repeat: Infinity }}
                />
              ) : (
                <XCircle className="h-4 w-4 text-white" />
              )}
              <span className="text-sm font-bold text-white">
                {isOpen ? "Abierto" : "Cerrado"}
              </span>
            </motion.div>
          </motion.div>
        </div>

        {/* Curved wave decoration */}
        <svg
          className="absolute -bottom-1 left-0 w-full"
          viewBox="0 0 1440 100"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          preserveAspectRatio="none"
        >
          <motion.path
            d="M0 100V60C180 90 360 30 540 40C720 50 900 90 1080 70C1260 50 1350 30 1440 50V100H0Z"
            className="fill-background"
            initial={{ d: "M0 100V80C180 80 360 80 540 80C720 80 900 80 1080 80C1260 80 1350 80 1440 80V100H0Z" }}
            animate={{ d: "M0 100V60C180 90 360 30 540 40C720 50 900 90 1080 70C1260 50 1350 30 1440 50V100H0Z" }}
            transition={{ duration: 1, delay: 0.5 }}
          />
        </svg>
      </motion.div>

      {/* Content */}
      <div className="flex-1 overflow-auto px-5 pb-28">
        {/* Section Header */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={mounted ? { opacity: 1, x: 0 } : {}}
          transition={{ delay: 0.7 }}
          className="mb-5 flex items-center gap-3"
        >
          <motion.div 
            className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/15 to-accent/15 shadow-sm"
            whileHover={{ scale: 1.1, rotate: 5 }}
          >
            <Flame className="h-6 w-6 text-primary" />
          </motion.div>
          <div>
            <h2 className="text-xl font-bold text-foreground">Opciones disponibles</h2>
            <p className="text-sm text-muted-foreground">Selecciona tu preferencia</p>
          </div>
        </motion.div>

        {/* Menu Options */}
        <div className="space-y-4">
          {options.map((option, index) => (
            <motion.div
              key={option.id}
              initial={{ opacity: 0, y: 30, scale: 0.95 }}
              animate={mounted ? { opacity: 1, y: 0, scale: 1 } : {}}
              transition={{ 
                delay: 0.8 + index * 0.15,
                type: "spring",
                stiffness: 150
              }}
            >
              <motion.button
                onClick={() => handleSelect(option.id)}
                disabled={!isOpen || hasSelected}
                whileHover={isOpen && !hasSelected ? { scale: 1.02, y: -4 } : {}}
                whileTap={isOpen && !hasSelected ? { scale: 0.98 } : {}}
                className={cn(
                  "group relative w-full overflow-hidden rounded-3xl text-left transition-all duration-500",
                  selectedOption === option.id
                    ? "shadow-glow"
                    : "shadow-premium hover:shadow-premium-lg"
                )}
              >
                {/* Card Background */}
                <div className={cn(
                  "relative p-5 transition-all duration-500",
                  selectedOption === option.id
                    ? "bg-gradient-to-br from-primary via-primary/95 to-accent"
                    : "bg-card"
                )}>
                  {/* Animated background pattern for selected */}
                  {selectedOption === option.id && (
                    <motion.div
                      className="absolute inset-0 opacity-20"
                      style={{
                        backgroundImage: `radial-gradient(circle at 20% 50%, white 1px, transparent 1px),
                                         radial-gradient(circle at 80% 20%, white 1px, transparent 1px),
                                         radial-gradient(circle at 40% 80%, white 1px, transparent 1px)`
                      }}
                      animate={{ 
                        backgroundPosition: ["0px 0px", "20px 20px", "0px 0px"]
                      }}
                      transition={{ duration: 4, repeat: Infinity }}
                    />
                  )}

                  {/* Selection indicator */}
                  <AnimatePresence>
                    {selectedOption === option.id && (
                      <motion.div
                        className="absolute right-4 top-4"
                        initial={{ scale: 0, rotate: -180 }}
                        animate={{ scale: 1, rotate: 0 }}
                        exit={{ scale: 0, rotate: 180 }}
                        transition={{ type: "spring", stiffness: 300 }}
                      >
                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-white shadow-lg">
                          <motion.div
                            initial={{ pathLength: 0 }}
                            animate={{ pathLength: 1 }}
                            transition={{ duration: 0.5, delay: 0.2 }}
                          >
                            <CheckCircle2 className="h-6 w-6 text-primary" />
                          </motion.div>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>

                  {/* Content */}
                  <div className={cn("relative pr-14", selectedOption === option.id && "pr-16")}>
                    {/* Option number badge */}
                    <motion.span 
                      className={cn(
                        "mb-2 inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold",
                        selectedOption === option.id 
                          ? "bg-white/20 text-white" 
                          : "bg-primary/10 text-primary"
                      )}
                      whileHover={{ scale: 1.1 }}
                    >
                      {index + 1}
                    </motion.span>
                    
                    <h3 className={cn(
                      "text-lg font-semibold leading-snug transition-colors",
                      selectedOption === option.id ? "text-white" : "text-foreground"
                    )}>
                      {option.name}
                    </h3>
                    
                    {/* Calories badge */}
                    {option.calories && (
                      <motion.div
                        className={cn(
                          "mt-2 inline-flex items-center gap-1.5 rounded-full px-3 py-1",
                          selectedOption === option.id 
                            ? "bg-white/15" 
                            : "bg-muted"
                        )}
                        initial={{ opacity: 0, x: -10 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 1 + index * 0.1 }}
                      >
                        <Flame className={cn(
                          "h-3.5 w-3.5",
                          selectedOption === option.id ? "text-white/80" : "text-muted-foreground"
                        )} />
                        <span className={cn(
                          "text-xs font-medium",
                          selectedOption === option.id ? "text-white/80" : "text-muted-foreground"
                        )}>
                          {option.calories}
                        </span>
                      </motion.div>
                    )}
                    
                    {option.selected && (
                      <motion.div
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 1.2 }}
                        className="mt-3 flex items-center gap-2"
                      >
                        <motion.div
                          animate={{ rotate: [0, 15, -15, 0] }}
                          transition={{ duration: 2, repeat: Infinity, repeatDelay: 1 }}
                        >
                          <Sparkles className={cn(
                            "h-4 w-4",
                            selectedOption === option.id ? "text-white/90" : "text-primary"
                          )} />
                        </motion.div>
                        <span className={cn(
                          "text-sm font-medium",
                          selectedOption === option.id ? "text-white/95" : "text-primary"
                        )}>
                          Tu seleccion
                        </span>
                      </motion.div>
                    )}
                  </div>

                  {/* Hover shine effect */}
                  {!hasSelected && isOpen && (
                    <motion.div
                      className="pointer-events-none absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent opacity-0 transition-opacity group-hover:opacity-100"
                      initial={{ x: "-100%" }}
                      whileHover={{ x: "200%" }}
                      transition={{ duration: 0.8 }}
                    />
                  )}
                </div>
              </motion.button>
            </motion.div>
          ))}
        </div>

        {/* Empty state */}
        {options.length === 0 && (
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="flex flex-col items-center justify-center py-20 text-center"
          >
            <motion.div
              className="mb-5 flex h-24 w-24 items-center justify-center rounded-full bg-gradient-to-br from-muted to-muted/50"
              animate={{ y: [0, -10, 0] }}
              transition={{ duration: 2.5, repeat: Infinity }}
            >
              <UtensilsCrossed className="h-12 w-12 text-muted-foreground" />
            </motion.div>
            <h3 className="text-xl font-bold text-foreground">Sin opciones</h3>
            <p className="mt-2 max-w-[200px] text-sm text-muted-foreground">
              El menu del dia aun no fue cargado
            </p>
          </motion.div>
        )}
      </div>
    </div>
  )
}
