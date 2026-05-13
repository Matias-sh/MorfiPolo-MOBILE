"use client"

import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { Eye, EyeOff, Lock, CheckCircle2, XCircle, Shield, Sparkles, X } from "lucide-react"
import { cn } from "@/lib/utils"

interface ChangePasswordDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSave?: (currentPassword: string, newPassword: string) => void
}

export function ChangePasswordDialog({
  open,
  onOpenChange,
  onSave,
}: ChangePasswordDialogProps) {
  const [currentPassword, setCurrentPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [focusedField, setFocusedField] = useState<string | null>(null)
  const [showSuccess, setShowSuccess] = useState(false)

  // Password validation
  const hasMinLength = newPassword.length >= 8
  const hasUppercase = /[A-Z]/.test(newPassword)
  const hasLowercase = /[a-z]/.test(newPassword)
  const hasNumber = /[0-9]/.test(newPassword)
  const passwordsMatch = newPassword === confirmPassword && confirmPassword.length > 0
  const isValid = hasMinLength && hasUppercase && hasLowercase && hasNumber && passwordsMatch && currentPassword.length > 0

  const validationProgress = [hasMinLength, hasUppercase, hasLowercase, hasNumber].filter(Boolean).length

  const handleSave = async () => {
    if (!isValid) return
    setIsLoading(true)
    await new Promise(resolve => setTimeout(resolve, 1500))
    setIsLoading(false)
    setShowSuccess(true)
    await new Promise(resolve => setTimeout(resolve, 1500))
    onSave?.(currentPassword, newPassword)
    handleClose()
  }

  const handleClose = () => {
    setCurrentPassword("")
    setNewPassword("")
    setConfirmPassword("")
    setShowSuccess(false)
    onOpenChange(false)
  }

  if (!open) return null

  return (
    <AnimatePresence>
      <motion.div
        className="fixed inset-0 z-50 flex items-end justify-center bg-black/60 backdrop-blur-sm sm:items-center"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={handleClose}
      >
        <motion.div
          className="relative w-full max-w-md overflow-hidden rounded-t-3xl bg-card shadow-2xl sm:rounded-3xl sm:mx-4"
          initial={{ y: 100, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 100, opacity: 0 }}
          transition={{ type: "spring", damping: 25, stiffness: 300 }}
          onClick={(e) => e.stopPropagation()}
        >
          {/* Success overlay */}
          <AnimatePresence>
            {showSuccess && (
              <motion.div
                className="absolute inset-0 z-20 flex flex-col items-center justify-center bg-card"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
              >
                <motion.div
                  className="mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-success"
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  transition={{ type: "spring", stiffness: 200 }}
                >
                  <motion.div
                    initial={{ pathLength: 0 }}
                    animate={{ pathLength: 1 }}
                    transition={{ duration: 0.5, delay: 0.2 }}
                  >
                    <CheckCircle2 className="h-10 w-10 text-white" />
                  </motion.div>
                </motion.div>
                <motion.h3
                  className="text-xl font-bold text-foreground"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.3 }}
                >
                  Contrasena actualizada
                </motion.h3>
                <motion.p
                  className="mt-2 text-sm text-muted-foreground"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.5 }}
                >
                  Tu contrasena fue cambiada exitosamente
                </motion.p>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Handle bar */}
          <div className="flex justify-center pt-3 sm:hidden">
            <div className="h-1 w-10 rounded-full bg-muted" />
          </div>

          {/* Close button */}
          <motion.button
            onClick={handleClose}
            className="absolute right-4 top-4 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-muted/80 text-muted-foreground hover:bg-muted"
            whileHover={{ scale: 1.1 }}
            whileTap={{ scale: 0.9 }}
          >
            <X className="h-4 w-4" />
          </motion.button>

          {/* Header */}
          <div className="px-6 pb-4 pt-6">
            <div className="flex items-center gap-3">
              <motion.div
                className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/10 to-accent/10"
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", delay: 0.1 }}
              >
                <Shield className="h-6 w-6 text-primary" />
              </motion.div>
              <div>
                <motion.h2
                  className="text-xl font-bold text-foreground"
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.2 }}
                >
                  Cambiar contrasena
                </motion.h2>
                <motion.p
                  className="text-sm text-muted-foreground"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.3 }}
                >
                  Actualiza tu contrasena de acceso
                </motion.p>
              </div>
            </div>
          </div>

          {/* Form */}
          <div className="space-y-4 px-6 pb-6">
            {/* Current Password */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
            >
              <label className="mb-2 block text-sm font-medium text-foreground">
                Contrasena actual
              </label>
              <div
                className={cn(
                  "relative overflow-hidden rounded-2xl border-2 bg-input transition-all duration-300",
                  focusedField === "current" 
                    ? "border-primary shadow-glow" 
                    : "border-transparent"
                )}
              >
                <div className="flex items-center gap-3 px-4 py-3">
                  <Lock className={cn(
                    "h-5 w-5 transition-colors",
                    focusedField === "current" ? "text-primary" : "text-muted-foreground"
                  )} />
                  <input
                    type={showCurrentPassword ? "text" : "password"}
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    onFocus={() => setFocusedField("current")}
                    onBlur={() => setFocusedField(null)}
                    placeholder="Ingresa tu contrasena actual"
                    className="flex-1 bg-transparent text-sm font-medium text-foreground outline-none placeholder:text-muted-foreground/60"
                  />
                  <button
                    type="button"
                    onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                    className="text-muted-foreground hover:text-foreground"
                  >
                    {showCurrentPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                  </button>
                </div>
              </div>
            </motion.div>

            {/* New Password */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
            >
              <label className="mb-2 block text-sm font-medium text-foreground">
                Nueva contrasena
              </label>
              <div
                className={cn(
                  "relative overflow-hidden rounded-2xl border-2 bg-input transition-all duration-300",
                  focusedField === "new" 
                    ? "border-primary shadow-glow" 
                    : "border-transparent"
                )}
              >
                <div className="flex items-center gap-3 px-4 py-3">
                  <Lock className={cn(
                    "h-5 w-5 transition-colors",
                    focusedField === "new" ? "text-primary" : "text-muted-foreground"
                  )} />
                  <input
                    type={showNewPassword ? "text" : "password"}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    onFocus={() => setFocusedField("new")}
                    onBlur={() => setFocusedField(null)}
                    placeholder="Ingresa tu nueva contrasena"
                    className="flex-1 bg-transparent text-sm font-medium text-foreground outline-none placeholder:text-muted-foreground/60"
                  />
                  <button
                    type="button"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                    className="text-muted-foreground hover:text-foreground"
                  >
                    {showNewPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                  </button>
                </div>
                
                {/* Progress bar */}
                {newPassword.length > 0 && (
                  <div className="h-1 bg-muted">
                    <motion.div
                      className={cn(
                        "h-full",
                        validationProgress <= 1 ? "bg-destructive" :
                        validationProgress <= 2 ? "bg-warning" :
                        validationProgress <= 3 ? "bg-info" : "bg-success"
                      )}
                      initial={{ width: 0 }}
                      animate={{ width: `${(validationProgress / 4) * 100}%` }}
                      transition={{ duration: 0.3 }}
                    />
                  </div>
                )}
              </div>

              {/* Password requirements */}
              {newPassword.length > 0 && (
                <motion.div
                  className="mt-3 grid grid-cols-2 gap-2"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: "auto" }}
                >
                  <RequirementItem met={hasMinLength} text="8+ caracteres" />
                  <RequirementItem met={hasUppercase} text="Mayuscula" />
                  <RequirementItem met={hasLowercase} text="Minuscula" />
                  <RequirementItem met={hasNumber} text="Numero" />
                </motion.div>
              )}
            </motion.div>

            {/* Confirm Password */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.5 }}
            >
              <label className="mb-2 block text-sm font-medium text-foreground">
                Confirmar contrasena
              </label>
              <div
                className={cn(
                  "relative overflow-hidden rounded-2xl border-2 bg-input transition-all duration-300",
                  focusedField === "confirm" 
                    ? "border-primary shadow-glow" 
                    : confirmPassword.length > 0 && !passwordsMatch
                    ? "border-destructive"
                    : "border-transparent"
                )}
              >
                <div className="flex items-center gap-3 px-4 py-3">
                  <Lock className={cn(
                    "h-5 w-5 transition-colors",
                    focusedField === "confirm" ? "text-primary" : "text-muted-foreground"
                  )} />
                  <input
                    type={showConfirmPassword ? "text" : "password"}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    onFocus={() => setFocusedField("confirm")}
                    onBlur={() => setFocusedField(null)}
                    placeholder="Repeti tu nueva contrasena"
                    className="flex-1 bg-transparent text-sm font-medium text-foreground outline-none placeholder:text-muted-foreground/60"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="text-muted-foreground hover:text-foreground"
                  >
                    {showConfirmPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                  </button>
                </div>
              </div>

              {/* Match indicator */}
              <AnimatePresence>
                {confirmPassword.length > 0 && (
                  <motion.div
                    className={cn(
                      "mt-2 flex items-center gap-1.5 text-xs font-medium",
                      passwordsMatch ? "text-success" : "text-destructive"
                    )}
                    initial={{ opacity: 0, y: -5 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -5 }}
                  >
                    {passwordsMatch ? (
                      <>
                        <CheckCircle2 className="h-3.5 w-3.5" />
                        Las contrasenas coinciden
                      </>
                    ) : (
                      <>
                        <XCircle className="h-3.5 w-3.5" />
                        Las contrasenas no coinciden
                      </>
                    )}
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>

            {/* Actions */}
            <motion.div
              className="flex gap-3 pt-4"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.6 }}
            >
              <motion.button
                onClick={handleClose}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="flex-1 rounded-2xl bg-muted py-3.5 text-sm font-semibold text-foreground"
              >
                Cancelar
              </motion.button>
              <motion.button
                onClick={handleSave}
                disabled={!isValid || isLoading}
                whileHover={isValid ? { scale: 1.02 } : {}}
                whileTap={isValid ? { scale: 0.98 } : {}}
                className={cn(
                  "relative flex-1 overflow-hidden rounded-2xl py-3.5 text-sm font-semibold text-white shadow-lg transition-all",
                  isValid 
                    ? "bg-gradient-to-r from-primary to-accent shadow-primary/25" 
                    : "bg-muted-foreground/30 cursor-not-allowed"
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
                        className="h-4 w-4 rounded-full border-2 border-white/30 border-t-white"
                        animate={{ rotate: 360 }}
                        transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                      />
                      Guardando...
                    </motion.div>
                  ) : (
                    <motion.span
                      key="text"
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      exit={{ opacity: 0 }}
                    >
                      Guardar
                    </motion.span>
                  )}
                </AnimatePresence>

                {/* Shimmer effect */}
                {isValid && !isLoading && (
                  <motion.div
                    className="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent"
                    initial={{ x: "-100%" }}
                    animate={{ x: "100%" }}
                    transition={{ duration: 1.5, repeat: Infinity, repeatDelay: 1 }}
                  />
                )}
              </motion.button>
            </motion.div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  )
}

function RequirementItem({ met, text }: { met: boolean; text: string }) {
  return (
    <motion.div
      className={cn(
        "flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-medium transition-all",
        met 
          ? "bg-success/10 text-success" 
          : "bg-muted text-muted-foreground"
      )}
      initial={{ scale: 0.9, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
    >
      {met ? (
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ type: "spring" }}
        >
          <CheckCircle2 className="h-3.5 w-3.5" />
        </motion.div>
      ) : (
        <div className="h-3.5 w-3.5 rounded-full border-2 border-current" />
      )}
      {text}
    </motion.div>
  )
}
