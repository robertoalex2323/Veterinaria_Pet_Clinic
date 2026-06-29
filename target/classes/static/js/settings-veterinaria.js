document.addEventListener("DOMContentLoaded", () => {
    const tabs = Array.from(document.querySelectorAll("[data-settings-tab]"));
    const panels = Array.from(document.querySelectorAll("[data-settings-panel]"));

    tabs.forEach(tab => {
        tab.addEventListener("click", () => {
            const target = tab.dataset.settingsTab;
            tabs.forEach(item => {
                const active = item === tab;
                item.classList.toggle("active", active);
                const chevron = item.querySelector(".option-chevron");
                if (chevron) {
                    chevron.className = active ? "fas fa-chevron-down option-chevron" : "fas fa-chevron-right option-chevron";
                }
            });
            panels.forEach(panel => panel.classList.toggle("active", panel.dataset.settingsPanel === target));
        });
    });

    const settings = window.vetThemeSettings || {};
    const current = settings.get ? settings.get() : { mode: "light", color: "lavender", text: "medium" };

    bindChoice("[data-theme-mode]", "themeMode", current.mode, value => settings.set && settings.set({ mode: value }));
    bindChoice("[data-theme-color]", "themeColor", current.color, value => settings.set && settings.set({ color: value }));
    bindChoice("[data-text-size]", "textSize", current.text, value => settings.set && settings.set({ text: value }));
});

function bindChoice(selector, datasetKey, initialValue, onChange) {
    const choices = Array.from(document.querySelectorAll(selector));
    const setActive = value => choices.forEach(choice => {
        choice.classList.toggle("active", choice.dataset[datasetKey] === value);
    });

    setActive(initialValue);
    choices.forEach(choice => {
        choice.addEventListener("click", () => {
            const value = choice.dataset[datasetKey];
            setActive(value);
            onChange(value);
        });
    });
}
