<template>
    <div>
        <NavBar />
        <router-view />
    </div>
</template>

<script>
import NavBar from './components/NavBar.vue';
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";

export default {
    name: 'App',
    components: {
        NavBar
    },
    // setup() {
    //     const store = useStore();
    //     const jwt_token = localStorage.getItem("jwt_token");

    //     if (jwt_token) {
    //         store.commit("updateToken", jwt_token);
    //         store.dispatch("getinfo", {
    //             success() {},
    //             error() {
    //                 store.dispatch("logout");
    //             }
    //         })
    //     }
    // }
}
</script>

<style>
/* ── Design tokens: shadcn-inspired light theme (global) ───────────────── */
:root {
    --tb-bg:        #f8fafc;   /* slate-50  : dashboard background */
    --tb-card:      #ffffff;   /* panel background */
    --tb-border:    #e2e8f0;   /* slate-200 : card borders */
    --tb-border-2:  #f1f5f9;   /* slate-100 : inner dividers */
    --tb-fg:        #0f172a;   /* slate-900 : primary text */
    --tb-muted:     #64748b;   /* slate-500 : labels */
    --tb-muted-2:   #94a3b8;   /* slate-400 : faint text */
    --tb-accent:    #4f46e5;   /* indigo-600 : primary accent */
    --tb-success:   #16a34a;   /* green-600 */
    --tb-danger:    #dc2626;   /* red-600 */
    --tb-warning:   #d97706;   /* amber-600 */
    --tb-radius:    10px;
    --tb-shadow:    0 1px 2px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(15, 23, 42, 0.06);
    --tb-font-ui:   'Inter', system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
    --tb-font-mono: 'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

body {
    /* background temporarily removed */
    background: var(--tb-bg);
    min-height: 100vh;
    margin: 0;
    color: var(--tb-fg);
    font-family: var(--tb-font-ui);
}
</style>
