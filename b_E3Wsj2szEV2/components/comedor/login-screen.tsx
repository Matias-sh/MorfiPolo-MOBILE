"use client"

import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { Eye, EyeOff, User, Lock, Utensils, Sparkles } from "lucide-react"
import { cn } from "@/lib/utils"

interface LoginScreenProps {
  onLogin?: () => void
}

export function LoginScreen({ onLogin }: LoginScreenProps) {
  const [dni, setDni] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [focusedField, setFocusedField] = useState<string | null>(null)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  const handleSubmit = async () => {
    setIsLoading(true)
    await new Promise(resolve => setTimeout(resolve, 1500))
    setIsLoading(false)
    onLogin?.()
  }

  return (
    <div className="relative flex h-full flex-col overflow-hidden bg-gradient-to-br from-primary/5 via-background to-accent/10">
      {/* Animated background shapes */}
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 0.15, scale: 1 }}
          transition={{ duration: 1.5, ease: "easeOut" }}
          className="absolute -right-20 -top-20 h-80 w-80 rounded-full bg-gradient-to-br from-primary to-primary/50 blur-3xl"
        />
        <motion.div
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 0.1, scale: 1 }}
          transition={{ duration: 1.5, delay: 0.3, ease: "easeOut" }}
          className="absolute -bottom-32 -left-32 h-96 w-96 rounded-full bg-gradient-to-tr from-accent to-primary/30 blur-3xl"
        />
      </div>

      {/* Content */}
      <div className="relative z-10 flex flex-1 flex-col items-center justify-center px-8">
        {/* Logo Section */}
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={mounted ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
          className="mb-10 flex flex-col items-center"
        >
          {/* Animated Logo */}
          <motion.div
            className="relative mb-6"
            whileHover={{ scale: 1.05 }}
            transition={{ type: "spring", stiffness: 300 }}
          >
            <div className="relative flex h-28 w-28 items-center justify-center">
              {/* Outer glow ring */}
              <motion.div
                className="absolute inset-0 rounded-full bg-gradient-to-br from-primary via-primary/80 to-accent"
                animate={{ 
                  boxShadow: [
                    "0 0 20px rgba(209, 99, 67, 0.3)",
                    "0 0 40px rgba(209, 99, 67, 0.5)",
                    "0 0 20px rgba(209, 99, 67, 0.3)"
                  ]
                }}
                transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
              />
              
              {/* Inner white circle */}
              <div className="absolute inset-2 rounded-full bg-white shadow-inner" />
              
              {/* Icon container */}
              <motion.div
                className="relative z-10 flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary/90"
                animate={{ rotate: [0, 5, -5, 0] }}
                transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
              >
                <Utensils className="h-10 w-10 text-white" strokeWidth={1.5} />
              </motion.div>
              
              {/* Floating sparkles */}
              <motion.div
                className="absolute -right-2 -top-1"
                animate={{ y: [-2, 2, -2], opacity: [1, 0.6, 1] }}
                transition={{ duration: 2, repeat: Infinity }}
              >
                <Sparkles className="h-5 w-5 text-primary/60" />
              </motion.div>
            </div>
          </motion.div>

          {/* Brand Text */}
          <motion.h1
            initial={{ opacity: 0 }}
            animate={mounted ? { opacity: 1 } : {}}
            transition={{ delay: 0.3, duration: 0.6 }}
            className="bg-gradient-to-r from-foreground via-foreground to-muted-foreground bg-clip-text text-3xl font-bold tracking-tight"
          >
            COMEDOR
          </motion.h1>
          <motion.p
            initial={{ opacity: 0 }}
            animate={mounted ? { opacity: 1 } : {}}
            transition={{ delay: 0.5, duration: 0.6 }}
            className="mt-1 text-sm text-muted-foreground"
          >
            Tu menu diario al alcance
          </motion.p>
        </motion.div>

        {/* Login Card */}
        <motion.div
          initial={{ opacity: 0, y: 40 }}
          animate={mounted ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.8, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
          className="w-full max-w-sm"
        >
          <div className="glass rounded-3xl p-6 shadow-premium">
            <motion.h2
              initial={{ opacity: 0 }}
              animate={mounted ? { opacity: 1 } : {}}
              transition={{ delay: 0.4 }}
              className="mb-1 text-center text-xl font-semibold text-foreground"
            >
              Iniciar Sesion
            </motion.h2>
            <motion.p
              initial={{ opacity: 0 }}
              animate={mounted ? { opacity: 1 } : {}}
              transition={{ delay: 0.5 }}
              className="mb-6 text-center text-sm text-muted-foreground"
            >
              Ingresa para ver el menu
            </motion.p>

            {/* Form Fields */}
            <div className="space-y-4">
              {/* DNI Field */}
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={mounted ? { opacity: 1, x: 0 } : {}}
                transition={{ delay: 0.5 }}
              >
                <div
                  className={cn(
                    "group relative overflow-hidden rounded-2xl border-2 bg-white/80 transition-all duration-300",
                    focusedField === "dni" 
                      ? "border-primary shadow-glow" 
                      : "border-transparent shadow-sm hover:border-primary/20"
                  )}
                >
                  <div className="flex items-center gap-3 px-4 py-3.5">
                    <div className={cn(
                      "flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-300",
                      focusedField === "dni" 
                        ? "bg-primary text-white" 
                        : "bg-muted text-muted-foreground"
                    )}>
                      <User className="h-5 w-5" />
                    </div>
                    <input
                      type="text"
                      placeholder="DNI"
                      value={dni}
                      onChange={(e) => setDni(e.target.value)}
                      onFocus={() => setFocusedField("dni")}
                      onBlur={() => setFocusedField(null)}
                      className="flex-1 bg-transparent text-base font-medium text-foreground outline-none placeholder:text-muted-foreground/60"
                    />
                  </div>
                  {/* Focus indicator line */}
                  <motion.div
                    className="absolute bottom-0 left-0 h-0.5 bg-gradient-to-r from-primary to-accent"
                    initial={{ width: "0%" }}
                    animate={{ width: focusedField === "dni" ? "100%" : "0%" }}
                    transition={{ duration: 0.3 }}
                  />
                </div>
              </motion.div>

              {/* Password Field */}
              <motion.div
                initial={{ opacity: 0, x: -20 }}
                animate={mounted ? { opacity: 1, x: 0 } : {}}
                transition={{ delay: 0.6 }}
              >
                <div
                  className={cn(
                    "group relative overflow-hidden rounded-2xl border-2 bg-white/80 transition-all duration-300",
                    focusedField === "password" 
                      ? "border-primary shadow-glow" 
                      : "border-transparent shadow-sm hover:border-primary/20"
                  )}
                >
                  <div className="flex items-center gap-3 px-4 py-3.5">
                    <div className={cn(
                      "flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-300",
                      focusedField === "password" 
                        ? "bg-primary text-white" 
                        : "bg-muted text-muted-foreground"
                    )}>
                      <Lock className="h-5 w-5" />
                    </div>
                    <input
                      type={showPassword ? "text" : "password"}
                      placeholder="Contrasena"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      onFocus={() => setFocusedField("password")}
                      onBlur={() => setFocusedField(null)}
                      className="flex-1 bg-transparent text-base font-medium text-foreground outline-none placeholder:text-muted-foreground/60"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="flex h-10 w-10 items-center justify-center rounded-xl text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                    >
                      {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                    </button>
                  </div>
                  <motion.div
                    className="absolute bottom-0 left-0 h-0.5 bg-gradient-to-r from-primary to-accent"
                    initial={{ width: "0%" }}
                    animate={{ width: focusedField === "password" ? "100%" : "0%" }}
                    transition={{ duration: 0.3 }}
                  />
                </div>
              </motion.div>

              {/* Login Button */}
              <motion.button
                initial={{ opacity: 0, y: 20 }}
                animate={mounted ? { opacity: 1, y: 0 } : {}}
                transition={{ delay: 0.7 }}
                whileHover={{ scale: 1.02, y: -2 }}
                whileTap={{ scale: 0.98 }}
                onClick={handleSubmit}
                disabled={isLoading || !dni || !password}
                className={cn(
                  "relative mt-2 w-full overflow-hidden rounded-2xl py-4 text-base font-semibold text-white shadow-lg transition-all duration-300",
                  isLoading || !dni || !password
                    ? "bg-muted-foreground/30 cursor-not-allowed"
                    : "bg-gradient-to-r from-primary via-primary to-accent hover:shadow-glow"
                )}
              >
                <AnimatePresence mode="wait">
                  {isLoading ? (
                    <motion.div
                      key="loading"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                      className="flex items-center justify-center gap-2"
                    >
                      <motion.div
                        className="h-5 w-5 rounded-full border-2 border-white/30 border-t-white"
                        animate={{ rotate: 360 }}
                        transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                      />
                      <span>Ingresando...</span>
                    </motion.div>
                  ) : (
                    <motion.span
                      key="text"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                    >
                      Ingresar
                    </motion.span>
                  )}
                </AnimatePresence>
                
                {/* Shimmer effect */}
                {!isLoading && dni && password && (
                  <motion.div
                    className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent"
                    initial={{ x: "-100%" }}
                    animate={{ x: "100%" }}
                    transition={{ duration: 1.5, repeat: Infinity, repeatDelay: 1 }}
                  />
                )}
              </motion.button>
            </div>

            {/* Help text */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={mounted ? { opacity: 1 } : {}}
              transition={{ delay: 0.9 }}
              className="mt-6 space-y-2 text-center"
            >
              <p className="text-xs text-muted-foreground">
                Para el primer ingreso utiliza como contrasena{" "}
                <span className="font-semibold text-primary">Ab</span> + tu DNI
              </p>
              <p className="text-xs text-muted-foreground">
                (Ej. Ab12345678)
              </p>
              <motion.p
                animate={{ opacity: [0.5, 1, 0.5] }}
                transition={{ duration: 2, repeat: Infinity }}
                className="pt-2 text-xs font-medium text-primary"
              >
                No olvides cambiar tu contrasena
              </motion.p>
            </motion.div>
          </div>
        </motion.div>
      </div>

      {/* Footer branding */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={mounted ? { opacity: 1 } : {}}
        transition={{ delay: 1 }}
        className="relative z-10 pb-6 text-center"
      >
        <p className="text-xs text-muted-foreground/60">
          Desarrollado por Direccion de Ingenieria
        </p>
      </motion.div>
    </div>
  )
}
