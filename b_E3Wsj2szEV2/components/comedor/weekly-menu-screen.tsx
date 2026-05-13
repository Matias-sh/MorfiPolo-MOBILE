"use client"

import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { Calendar, CheckCircle2, ChevronRight, Clock, Sparkles, XCircle } from "lucide-react"
import { cn } from "@/lib/utils"

interface DayMenu {
  date: string
  formattedDate: string
  dayName: string
  option: string
  isOpen: boolean
  hasSelected: boolean
  isPast: boolean
}

interface WeeklyMenuScreenProps {
  menus?: DayMenu[]
  onSelectDay?: (date: string) => void
}

export function WeeklyMenuScreen({
  menus = [
    { date: "2026-04-28", formattedDate: "28/04", dayName: "Lunes", option: "Muslo deshuesado con papas, calabaza y cebollas", isOpen: false, hasSelected: true, isPast: false },
    { date: "2026-04-27", formattedDate: "27/04", dayName: "Domingo", option: "Guiso de arroz, carne y lentejas", isOpen: false, hasSelected: true, isPast: true },
    { date: "2026-04-24", formattedDate: "24/04", dayName: "Jueves", option: "Albondigas con pure de papas", isOpen: false, hasSelected: true, isPast: true },
    { date: "2026-04-23", formattedDate: "23/04", dayName: "Miercoles", option: "Fideos con estofado de pollo", isOpen: false, hasSelected: true, isPast: true },
    { date: "2026-04-22", formattedDate: "22/04", dayName: "Martes", option: "Suprema de pollo con arroz primavera", isOpen: false, hasSelected: false, isPast: true },
  ],
  onSelectDay,
}: WeeklyMenuScreenProps) {
  const [mounted, setMounted] = useState(false)
  const [selectedCard, setSelectedCard] = useState<string | null>(null)

  useEffect(() => {
    setMounted(true)
  }, [])

  return (
    <div className="relative flex h-full flex-col overflow-hidden bg-background">
      {/* Hero Header */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={mounted ? { opacity: 1 } : {}}
        className="relative overflow-hidden"
      >
        {/* Gradient Background with animation */}
        <motion.div 
          className="absolute inset-0 bg-gradient-to-br from-primary via-primary/95 to-accent/80"
          animate={{
            backgroundPosition: ["0% 0%", "100% 100%", "0% 0%"],
          }}
          transition={{ duration: 15, repeat: Infinity, ease: "linear" }}
          style={{ backgroundSize: "200% 200%" }}
        />
        
        {/* Floating decorative elements */}
        <motion.div
          className="absolute -right-12 top-0 h-40 w-40 rounded-full bg-white/10"
          animate={{ 
            y: [0, 20, 0],
            scale: [1, 1.1, 1]
          }}
          transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
        />
        <motion.div
          className="absolute -bottom-10 -left-10 h-32 w-32 rounded-full bg-white/5"
          animate={{ 
            x: [0, 15, 0],
            scale: [1, 1.15, 1]
          }}
          transition={{ duration: 5, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
        />
        
        {/* Calendar icon floating */}
        <motion.div
          className="absolute right-8 top-6"
          animate={{ y: [-3, 3, -3], rotate: [0, 5, 0] }}
          transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
        >
          <Calendar className="h-6 w-6 text-white/25" />
        </motion.div>

        <div className="relative px-6 pb-10 pt-6">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={mounted ? { opacity: 1, y: 0 } : {}}
            transition={{ delay: 0.2 }}
            className="text-center"
          >
            <motion.div
              className="mb-3 inline-flex items-center justify-center rounded-2xl bg-white/15 px-4 py-2 backdrop-blur-sm"
              whileHover={{ scale: 1.05 }}
            >
              <Calendar className="mr-2 h-5 w-5 text-white/80" />
              <span className="text-sm font-medium text-white/90">Historial</span>
            </motion.div>
            
            <motion.h1
              className="text-3xl font-bold tracking-tight text-white"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={mounted ? { opacity: 1, scale: 1 } : {}}
              transition={{ delay: 0.3, type: "spring" }}
            >
              Menu Semanal
            </motion.h1>
            <motion.p
              className="mt-2 text-sm text-white/70"
              initial={{ opacity: 0 }}
              animate={mounted ? { opacity: 1 } : {}}
              transition={{ delay: 0.5 }}
            >
              Selecciona una fecha para ver detalles
            </motion.p>
          </motion.div>
        </div>

        {/* Wave decoration */}
        <svg
          className="absolute -bottom-1 left-0 w-full"
          viewBox="0 0 1440 80"
          fill="none"
          preserveAspectRatio="none"
        >
          <motion.path
            d="M0 80V40C240 70 480 20 720 35C960 50 1200 70 1440 50V80H0Z"
            className="fill-background"
            initial={{ d: "M0 80V60C240 60 480 60 720 60C960 60 1200 60 1440 60V80H0Z" }}
            animate={{ d: "M0 80V40C240 70 480 20 720 35C960 50 1200 70 1440 50V80H0Z" }}
            transition={{ duration: 0.8, delay: 0.4 }}
          />
        </svg>
      </motion.div>

      {/* Content */}
      <div className="flex-1 overflow-auto px-5 pb-28">
        {/* Timeline indicator */}
        <div className="relative">
          {/* Vertical line */}
          <motion.div
            className="absolute left-6 top-8 bottom-8 w-0.5 bg-gradient-to-b from-primary/30 via-primary/10 to-transparent"
            initial={{ scaleY: 0 }}
            animate={mounted ? { scaleY: 1 } : {}}
            transition={{ delay: 0.6, duration: 0.8 }}
            style={{ transformOrigin: "top" }}
          />

          {/* Menu Cards */}
          <div className="space-y-4">
            {menus.map((menu, index) => (
              <motion.div
                key={menu.date}
                initial={{ opacity: 0, x: -30 }}
                animate={mounted ? { opacity: 1, x: 0 } : {}}
                transition={{ 
                  delay: 0.7 + index * 0.1,
                  type: "spring",
                  stiffness: 150
                }}
                className="relative"
              >
                {/* Timeline dot */}
                <motion.div
                  className={cn(
                    "absolute left-4 top-6 z-10 h-4 w-4 rounded-full border-2 border-background",
                    index === 0 ? "bg-primary" : menu.hasSelected ? "bg-success" : "bg-muted-foreground/30"
                  )}
                  initial={{ scale: 0 }}
                  animate={mounted ? { scale: 1 } : {}}
                  transition={{ delay: 0.8 + index * 0.1, type: "spring" }}
                >
                  {index === 0 && (
                    <motion.div
                      className="absolute inset-0 rounded-full bg-primary"
                      animate={{ scale: [1, 1.5, 1], opacity: [1, 0, 1] }}
                      transition={{ duration: 2, repeat: Infinity }}
                    />
                  )}
                </motion.div>

                {/* Card */}
                <motion.button
                  onClick={() => {
                    setSelectedCard(menu.date)
                    onSelectDay?.(menu.date)
                  }}
                  whileHover={{ scale: 1.02, x: 4 }}
                  whileTap={{ scale: 0.98 }}
                  className={cn(
                    "ml-10 w-[calc(100%-2.5rem)] overflow-hidden rounded-2xl text-left transition-all duration-300",
                    index === 0 
                      ? "bg-card shadow-premium-lg ring-2 ring-primary/20" 
                      : "bg-card shadow-premium hover:shadow-premium-lg"
                  )}
                >
                  <div className="relative p-4">
                    {/* Header row */}
                    <div className="mb-3 flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        {/* Date badge */}
                        <div className={cn(
                          "flex h-12 w-12 flex-col items-center justify-center rounded-xl",
                          index === 0 
                            ? "bg-gradient-to-br from-primary to-accent text-white" 
                            : "bg-muted text-muted-foreground"
                        )}>
                          <span className="text-lg font-bold leading-none">
                            {menu.formattedDate.split("/")[0]}
                          </span>
                          <span className="text-[10px] uppercase opacity-80">
                            {menu.dayName.slice(0, 3)}
                          </span>
                        </div>
                        
                        <div>
                          <span className="text-sm font-semibold text-foreground">
                            {menu.dayName}
                          </span>
                          <div className="flex items-center gap-2">
                            <span className="text-xs text-muted-foreground">
                              {menu.formattedDate}
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Status badge */}
                      <motion.div
                        className={cn(
                          "flex items-center gap-1 rounded-full px-2.5 py-1",
                          menu.isOpen 
                            ? "bg-success/10 text-success" 
                            : "bg-muted text-muted-foreground"
                        )}
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ delay: 1 + index * 0.05 }}
                      >
                        {menu.isOpen ? (
                          <motion.div
                            className="h-1.5 w-1.5 rounded-full bg-success"
                            animate={{ scale: [1, 1.3, 1] }}
                            transition={{ duration: 1.5, repeat: Infinity }}
                          />
                        ) : (
                          <Clock className="h-3 w-3" />
                        )}
                        <span className="text-[10px] font-semibold uppercase">
                          {menu.isOpen ? "Abierto" : "Cerrado"}
                        </span>
                      </motion.div>
                    </div>

                    {/* Menu option */}
                    <p className="mb-3 text-sm font-medium leading-snug text-foreground line-clamp-2">
                      {menu.option}
                    </p>

                    {/* Footer */}
                    <div className="flex items-center justify-between">
                      {menu.hasSelected ? (
                        <motion.div
                          className="flex items-center gap-1.5 text-primary"
                          initial={{ opacity: 0, x: -10 }}
                          animate={{ opacity: 1, x: 0 }}
                          transition={{ delay: 1.1 + index * 0.05 }}
                        >
                          <CheckCircle2 className="h-4 w-4" />
                          <span className="text-xs font-semibold">Seleccionado</span>
                        </motion.div>
                      ) : menu.isPast ? (
                        <div className="flex items-center gap-1.5 text-muted-foreground">
                          <XCircle className="h-4 w-4" />
                          <span className="text-xs">Sin seleccion</span>
                        </div>
                      ) : (
                        <div className="flex items-center gap-1.5 text-warning">
                          <Sparkles className="h-4 w-4" />
                          <span className="text-xs font-medium">Pendiente</span>
                        </div>
                      )}

                      <motion.div
                        className="flex h-8 w-8 items-center justify-center rounded-full bg-muted/50"
                        whileHover={{ scale: 1.1, backgroundColor: "var(--primary)", color: "white" }}
                      >
                        <ChevronRight className="h-4 w-4" />
                      </motion.div>
                    </div>

                    {/* Current day indicator */}
                    {index === 0 && (
                      <motion.div
                        className="absolute -right-8 -top-8 h-16 w-16 rounded-full bg-primary/5"
                        animate={{ scale: [1, 1.2, 1] }}
                        transition={{ duration: 3, repeat: Infinity }}
                      />
                    )}
                  </div>
                </motion.button>
              </motion.div>
            ))}
          </div>
        </div>

        {/* Empty state */}
        {menus.length === 0 && (
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
              <Calendar className="h-12 w-12 text-muted-foreground" />
            </motion.div>
            <h3 className="text-xl font-bold text-foreground">Sin menus</h3>
            <p className="mt-2 max-w-[200px] text-sm text-muted-foreground">
              No hay menus cargados para mostrar
            </p>
          </motion.div>
        )}
      </div>
    </div>
  )
}
