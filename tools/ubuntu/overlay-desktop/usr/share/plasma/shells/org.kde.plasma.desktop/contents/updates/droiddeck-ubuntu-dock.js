panels().forEach(function (panel) {
    if (panel.location === "bottom" && panel.widgets("org.kde.plasma.kickoff").length > 0) {
        panel.location = "left";
        panel.height = 2 * Math.ceil(gridUnit * 2.75 / 2);
    }
});
