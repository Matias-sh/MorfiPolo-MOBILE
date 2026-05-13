"use client"

import { useState } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { MobileFrame } from "@/components/comedor/mobile-frame"
import { BottomNav } from "@/components/comedor/bottom-nav"
import { LoginScreen } from "@/components/comedor/login-screen"
import { DailyMenuScreen } from "@/components/comedor/daily-menu-screen"
import { WeeklyMenuScreen } from "@/components/comedor/weekly-menu-screen"
import { ProfileScreen } from "@/components/comedor/profile-screen"
import { ChangePasswordDialog } from "@/components/comedor/change-password-dialog"
import { cn } from "@/lib/utils"
import { Utensils, Sparkles, Code2, Palette, Smartphone, ChevronRight } from "lucide-react"

type Screen = "login" | "app"
type Tab = "daily" | "weekly" | "profile"

export default function Home() {
  const [currentScreen, setCurrentScreen] = useState<Screen>("login")
  const [activeTab, setActiveTab] = useState<Tab>("daily")
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [showChangePassword, setShowChangePassword] = useState(false)

  const handleLogin = () => {
    setIsLoggedIn(true)
    setCurrentScreen("app")
    setActiveTab("daily")
  }

  const handleLogout = () => {
    setIsLoggedIn(false)
    setCurrentScreen("login")
  }

  const screenButtons = [
    { id: "login", label: "Login", screen: "login" as Screen, tab: null },
    { id: "daily", label: "Menu del Dia", screen: "app" as Screen, tab: "daily" as Tab },
    { id: "weekly", label: "Menu Semanal", screen: "app" as Screen, tab: "weekly" as Tab },
    { id: "profile", label: "Perfil", screen: "app" as Screen, tab: "profile" as Tab },
  ]

  const isActiveButton = (id: string) => {
    if (id === "login") return currentScreen === "login"
    return currentScreen === "app" && activeTab === id
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-accent/5">
      {/* Hero Header */}
      <div className="relative overflow-hidden border-b border-border/50 bg-gradient-to-r from-card via-card to-accent/5">
        {/* Decorative background elements */}
        <div className="pointer-events-none absolute inset-0">
          <motion.div
            className="absolute -right-40 -top-40 h-96 w-96 rounded-full bg-gradient-to-br from-primary/10 to-accent/5 blur-3xl"
            animate={{ 
              scale: [1, 1.1, 1],
              rotate: [0, 45, 0]
            }}
            transition={{ duration: 20, repeat: Infinity }}
          />
          <motion.div
            className="absolute -bottom-20 -left-20 h-64 w-64 rounded-full bg-gradient-to-tr from-accent/10 to-primary/5 blur-3xl"
            animate={{ 
              scale: [1, 1.2, 1],
              x: [0, 20, 0]
            }}
            transition={{ duration: 15, repeat: Infinity, delay: 2 }}
          />
        </div>

        <div className="relative mx-auto max-w-6xl px-6 py-12">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="flex flex-col items-start gap-6 md:flex-row md:items-center md:justify-between"
          >
            {/* Logo and Title */}
            <div className="flex items-center gap-4">
              <motion.div
                className="relative flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-accent shadow-lg shadow-primary/25"
                whileHover={{ scale: 1.05, rotate: 5 }}
              >
                <Utensils className="h-8 w-8 text-white" />
                <motion.div
                  className="absolute -right-1 -top-1"
                  animate={{ rotate: [0, 15, -15, 0] }}
                  transition={{ duration: 3, repeat: Infinity }}
                >
                  <Sparkles className="h-5 w-5 text-warning" />
                </motion.div>
              </motion.div>
              
              <div>
                <motion.h1 
                  className="text-3xl font-bold tracking-tight text-foreground md:text-4xl"
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.2 }}
                >
                  Comedor App
                </motion.h1>
                <motion.p 
                  className="mt-1 text-muted-foreground"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.4 }}
                >
                  Rediseno Premium para Android
                </motion.p>
              </div>
            </div>

            {/* Feature badges */}
            <motion.div
              className="flex flex-wrap gap-3"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.5 }}
            >
              <FeatureBadge icon={Palette} label="Material 3" />
              <FeatureBadge icon={Smartphone} label="Android XML" />
              <FeatureBadge icon={Code2} label="Cursor Ready" />
            </motion.div>
          </motion.div>

          {/* Screen Selector */}
          <motion.div
            className="mt-8 flex flex-wrap gap-2"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.6 }}
          >
            {screenButtons.map((btn, index) => (
              <motion.button
                key={btn.id}
                onClick={() => {
                  setCurrentScreen(btn.screen)
                  if (btn.tab) {
                    setActiveTab(btn.tab)
                    setIsLoggedIn(true)
                  } else {
                    setIsLoggedIn(false)
                  }
                }}
                whileHover={{ scale: 1.03, y: -2 }}
                whileTap={{ scale: 0.97 }}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.7 + index * 0.1 }}
                className={cn(
                  "group relative overflow-hidden rounded-2xl px-5 py-2.5 text-sm font-semibold transition-all duration-300",
                  isActiveButton(btn.id)
                    ? "bg-gradient-to-r from-primary to-accent text-white shadow-lg shadow-primary/30"
                    : "bg-card text-foreground shadow-sm hover:shadow-md"
                )}
              >
                <span className="relative z-10">{btn.label}</span>
                
                {/* Hover shine effect */}
                {!isActiveButton(btn.id) && (
                  <motion.div
                    className="absolute inset-0 bg-gradient-to-r from-transparent via-primary/10 to-transparent opacity-0 group-hover:opacity-100"
                    initial={{ x: "-100%" }}
                    whileHover={{ x: "100%" }}
                    transition={{ duration: 0.6 }}
                  />
                )}
              </motion.button>
            ))}
          </motion.div>
        </div>
      </div>

      {/* Main Content */}
      <div className="mx-auto max-w-6xl px-6 py-12">
        <div className="flex flex-col items-center gap-12 lg:flex-row lg:items-start lg:justify-center">
          {/* Mobile Preview */}
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.3 }}
            className="relative"
          >
            {/* Glow effect */}
            <div className="absolute -inset-4 rounded-[60px] bg-gradient-to-br from-primary/20 via-transparent to-accent/20 blur-2xl" />
            
            <MobileFrame>
              <AnimatePresence mode="wait">
                {!isLoggedIn ? (
                  <motion.div
                    key="login"
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: 20 }}
                    className="h-full"
                  >
                    <LoginScreen onLogin={handleLogin} />
                  </motion.div>
                ) : (
                  <motion.div
                    key="app"
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -20 }}
                    className="relative h-full"
                  >
                    <AnimatePresence mode="wait">
                      {activeTab === "daily" && (
                        <motion.div
                          key="daily"
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          exit={{ opacity: 0 }}
                          className="h-full"
                        >
                          <DailyMenuScreen />
                        </motion.div>
                      )}
                      {activeTab === "weekly" && (
                        <motion.div
                          key="weekly"
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          exit={{ opacity: 0 }}
                          className="h-full"
                        >
                          <WeeklyMenuScreen />
                        </motion.div>
                      )}
                      {activeTab === "profile" && (
                        <motion.div
                          key="profile"
                          initial={{ opacity: 0 }}
                          animate={{ opacity: 1 }}
                          exit={{ opacity: 0 }}
                          className="h-full"
                        >
                          <ProfileScreen 
                            onChangePassword={() => setShowChangePassword(true)}
                            onLogout={handleLogout}
                          />
                        </motion.div>
                      )}
                    </AnimatePresence>

                    <BottomNav 
                      activeTab={activeTab} 
                      onTabChange={setActiveTab} 
                    />
                  </motion.div>
                )}
              </AnimatePresence>

              <ChangePasswordDialog 
                open={showChangePassword}
                onOpenChange={setShowChangePassword}
              />
            </MobileFrame>
          </motion.div>

          {/* Implementation Guide */}
          <motion.div
            className="w-full max-w-xl"
            initial={{ opacity: 0, x: 30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.5 }}
          >
            <div className="rounded-3xl bg-card p-6 shadow-premium">
              <div className="mb-6 flex items-center gap-3">
                <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-primary/10 to-accent/10">
                  <Code2 className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-foreground">Guia de Implementacion</h2>
                  <p className="text-sm text-muted-foreground">Android XML + Material 3</p>
                </div>
              </div>

              <div className="space-y-4">
                <ImplementationSection
                  title="Colores (colors.xml)"
                  code={`<!-- Primary - Rich Coral -->
<color name="md_theme_primary">#D16343</color>
<color name="md_theme_onPrimary">#FFFFFF</color>
<color name="md_theme_primaryContainer">#FFDBCF</color>

<!-- Success -->
<color name="success">#2E8B57</color>

<!-- Warning -->  
<color name="warning">#DAA520</color>

<!-- Error -->
<color name="error">#B8352E</color>

<!-- Surfaces -->
<color name="md_theme_background">#FBF9F6</color>
<color name="md_theme_surface">#FFFFFF</color>
<color name="md_theme_onSurface">#2D2520</color>`}
                />

                <ImplementationSection
                  title="Dimensiones (dimens.xml)"
                  code={`<!-- Spacing Scale -->
<dimen name="spacing_xs">4dp</dimen>
<dimen name="spacing_sm">8dp</dimen>
<dimen name="spacing_md">12dp</dimen>
<dimen name="spacing_lg">16dp</dimen>
<dimen name="spacing_xl">24dp</dimen>

<!-- Border Radius -->
<dimen name="radius_sm">12dp</dimen>
<dimen name="radius_md">16dp</dimen>
<dimen name="radius_lg">20dp</dimen>
<dimen name="radius_xl">28dp</dimen>

<!-- Shadows -->
<dimen name="elevation_sm">2dp</dimen>
<dimen name="elevation_md">4dp</dimen>
<dimen name="elevation_lg">8dp</dimen>`}
                />

                <ImplementationSection
                  title="Animaciones"
                  code={`<!-- res/anim/scale_up.xml -->
<set xmlns:android="...">
  <scale
    android:fromXScale="0.95"
    android:toXScale="1.0"
    android:fromYScale="0.95"
    android:toYScale="1.0"
    android:pivotX="50%"
    android:pivotY="50%"
    android:duration="300"
    android:interpolator="@android:anim/decelerate" />
  <alpha
    android:fromAlpha="0.0"
    android:toAlpha="1.0"
    android:duration="300" />
</set>`}
                />
              </div>
            </div>
          </motion.div>
        </div>
      </div>

      {/* Footer */}
      <div className="border-t border-border/50 bg-card/50 py-6 text-center">
        <p className="text-sm text-muted-foreground">
          Desarrollado por{" "}
          <span className="font-semibold text-foreground">Direccion de Ingenieria</span>
          {" "}- Gobierno de Formosa
        </p>
      </div>
    </div>
  )
}

function FeatureBadge({ icon: Icon, label }: { icon: typeof Palette; label: string }) {
  return (
    <motion.div
      whileHover={{ scale: 1.05 }}
      className="flex items-center gap-2 rounded-full bg-muted/50 px-4 py-2"
    >
      <Icon className="h-4 w-4 text-primary" />
      <span className="text-xs font-semibold text-foreground">{label}</span>
    </motion.div>
  )
}

function ImplementationSection({ title, code }: { title: string; code: string }) {
  const [isExpanded, setIsExpanded] = useState(false)

  return (
    <motion.div
      className="overflow-hidden rounded-2xl border border-border bg-muted/30"
      initial={false}
    >
      <button
        onClick={() => setIsExpanded(!isExpanded)}
        className="flex w-full items-center justify-between p-4 text-left"
      >
        <span className="font-semibold text-foreground">{title}</span>
        <motion.div
          animate={{ rotate: isExpanded ? 90 : 0 }}
          transition={{ duration: 0.2 }}
        >
          <ChevronRight className="h-5 w-5 text-muted-foreground" />
        </motion.div>
      </button>
      
      <AnimatePresence>
        {isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
          >
            <pre className="overflow-x-auto bg-foreground/95 p-4 text-xs text-background">
              <code>{code}</code>
            </pre>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  )
}
