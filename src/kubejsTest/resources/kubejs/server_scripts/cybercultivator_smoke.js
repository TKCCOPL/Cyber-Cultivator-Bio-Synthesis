ServerEvents.recipes(event => {
    event.recipes.cybercultivator.serum_bottling(
        'minecraft:diamond',
        ['minecraft:dirt']
    )
        .processingTime(1)
        .priority(100)
        .id('kubejs:cybercultivator_serum_smoke')

    event.recipes.cybercultivator.serum_bottling(
        'minecraft:gold_ingot',
        ['minecraft:dirt']
    )
        .processingTime(1)
        .priority(0)
        .id('kubejs:cybercultivator_serum_fallback_smoke')

    event.recipes.cybercultivator.incubator_output(
        'minecraft:emerald',
        'minecraft:wheat_seeds'
    )
        .countFormula('1')
        .qualityTag('Potency')
        .defaultGenes({ speed: 5, yield: 5, potency: 5 })
        .cropName('KubeJS Smoke Crop')
        .priority(100)
        .id('kubejs:cybercultivator_incubator_smoke')
})

CyberCultivatorEvents.geneSplice(event => {
    if (event.getGeneration() === 777) {
        event.setSpeed(9)
        event.setOffspringCount(2)
    }
})

CyberCultivatorEvents.cropMature(event => {
    if (event.getPos().getY() === 77) {
        event.cancel()
    }
})

CyberCultivatorEvents.serumCraft(event => {
    if (`${event.getRecipeId()}` === 'kubejs:cybercultivator_serum_smoke') {
        event.setActivity(12)
        event.setOutput('minecraft:gold_ingot')
    }
})

CyberCultivatorEvents.serumConsume(event => {
    if (event.getDuration() === 777) {
        event.setDuration(42)
    }
})
