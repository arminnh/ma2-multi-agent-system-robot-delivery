import json
from pprint import pprint
import glob
import re
import pandas as pd
import seaborn as sns

class ExperimentResults:

    def __init__(self, expId, parameters, results):
        self.expId = expId
        self.parameters = parameters
        tempResults = []
        for v in results:
            tempResults.append(v.update(parameters))

        self.results = pd.DataFrame(tempResults)


def getAllExperiments():
    experiments = {}
    for file in glob.glob("experiment-*.json"):

        fileName = file.split("/")[-1]
        experimentNr = re.match("experiment-([\d]+).json", fileName).group(1)
        with open(file) as f:
            e = json.load(f)

            experiments[experimentNr] = ExperimentResults(e["experiment"], e["parameters"], e["results"])

    return experiments

def drawBoxPlotForExperiments(expList, x,y):
    ax = sns.boxplot(x=x, y=y, data=expList)
    ax.savefig("test.png")

def getExperimentResultsForIds(exps, ids):
    retVal = []
    for i in ids:
        retVal.append(exps[str(i)].results)
    return pd.concat(retVal)

if __name__ == "__main__":

    experiments = getAllExperiments()

    firstExps = getExperimentResultsForIds(experiments, [1,2,3,4])
    drawBoxPlotForExperiments(firstExps, "probNewDeliveryTask", "totalTasksWaitingTime")
    #pprint(experiment1)
    #print("experiment1[experiment]:", experiment1["experiment"])
    #print("experiment1[parameters][numRobots]:", experiment1["parameters"]["numRobots"])
    #print("experiment1[results][0][totalTasksWaitingTime]:", experiment1["results"][0]["totalTasksWaitingTime"])
