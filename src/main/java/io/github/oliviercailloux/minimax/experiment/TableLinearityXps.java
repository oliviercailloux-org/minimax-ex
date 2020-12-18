package io.github.oliviercailloux.minimax.experiment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;
import com.google.common.math.Stats;

import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.j_voting.profiles.ProfileI;
import io.github.oliviercailloux.j_voting.profiles.management.ReadProfile;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.utils.Generator;

public class TableLinearityXps {
	private static final Logger LOGGER = LoggerFactory.getLogger(TableLinearityXps.class);

	public static void main(String[] args) throws Exception {
		final TableLinearityXps tableLinXps = new TableLinearityXps();
//		tableLinXps.runWithOracles(m, n, k, nbRuns);
//		tableLinXps.runWithOracles(5, 10, 100, 10);
//		tableLinXps.runWithOracles(5, 15, 150, 10);
//		tableLinXps.runWithOracles(5, 20, 200, 10);
//		tableLinXps.runWithOracles(10, 20, 500, 10);
//		tableLinXps.runWithOracles(10, 30, 800, 10);
//		tableLinXps.runWithOracles(15, 30, 1000, 1, 5);
//		tableLinXps.runWithOracles(15, 30, 1000, 1, 6);
		tableLinXps.runWithFile("courses.soc", 10, 1);
	}

	public void runWithOracles(int m, int n, int k, int nbRuns) throws IOException {
		StrategyFactory factory = StrategyFactory.limited();

		final Path json = Path.of("experiments/Oracles/",
				String.format("Oracles m = %d, n = %d, %d, %d.json", m, n, nbRuns));
		final ImmutableList<Oracle> oracles = ImmutableList.copyOf(JsonConverter.toOracles(Files.readString(json)));
		final String prefixDescription = factory.getDescription() + ", m = " + m + ", n = " + n + ", k = " + k;

		final Runs runs = runs(factory, oracles, k, prefixDescription, nbRuns);
		final Stats stats = runs.getMinimalMaxRegretStats().get(runs.getK());
		final String descr = Runner.asStringEstimator(stats);
		LOGGER.info("Got final estimator: {}.", descr);
	}

	public void runWithFile(String file, int k, int nbRuns) throws IOException {
		StrategyFactory factory = StrategyFactory.limited();

		String path = "experiments/Oracles/" + file;
		System.out.println(path);
		try (InputStream socStream = new FileInputStream(path)) {
			final ProfileI p = new ReadProfile().createProfileFromStream(socStream);
			LinkedList<VoterStrictPreference> profile = new LinkedList<>();
			for (Voter voter : p.getAllVoters()) {
				VoterStrictPreference vpref = VoterStrictPreference.given(voter,
						p.getPreference(voter).toStrictPreference().getAlternatives());
				profile.add(vpref);
			}
			final Oracle o = Oracle.build(profile, Generator.genWeightsWithUniformDistribution(p.getNbAlternatives()));
			System.out.println(o.toString());
			final ImmutableList<Oracle> oracles = ImmutableList.of(o);
			final String prefixDescription = factory.getDescription() + ", " + file + ", k = " + k ;

			final Runs runs = runs(factory, oracles, k, prefixDescription, nbRuns);
			final Stats stats = runs.getMinimalMaxRegretStats().get(runs.getK());
			final String descr = Runner.asStringEstimator(stats);
			LOGGER.info("Got final estimator: {}.", descr);
		}

	}

	public Runs runs(StrategyFactory factory, ImmutableList<Oracle> oracles, int k, String prefixDescription,
			int nbRuns) throws IOException {
		final Path outDir = Path.of("experiments/TableLinearity");
		Files.createDirectories(outDir);
		final String prefixTemp = prefixDescription + ", ongoing";
		final Path tmpJson = outDir.resolve(prefixTemp + ".json");
		final Path tmpCsv = outDir.resolve(prefixTemp + ".csv");

		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		LOGGER.info("Started '{}'.", factory.getDescription());
		for (int i = 0; i < nbRuns; ++i) {
			final Oracle oracle = oracles.get(i);
			LOGGER.info("Before run.");
			final Run run = Runner.run(factory, oracle, k);
			LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
			runsBuilder.add(run);
			final Runs runs = Runs.of(factory, runsBuilder.build());
			LOGGER.info("Runs.");
			// Runner.summarize(runs);
			Files.writeString(tmpJson, JsonConverter.toJson(runs).toString());
			LOGGER.info("Written json.");
			Files.writeString(tmpCsv, ToCsv.toCsv(runs, 1));
			LOGGER.info("Written csv.");
		}

		final String prefix = prefixDescription + ", nbRuns = " + nbRuns;
		final Path outJson = outDir.resolve(prefix + ".json");
		final Path outCsv = outDir.resolve(prefix + ".csv");
		Files.move(tmpJson, outJson, StandardCopyOption.REPLACE_EXISTING);
		Files.move(tmpCsv, outCsv, StandardCopyOption.REPLACE_EXISTING);

		return Runs.of(factory, runsBuilder.build());
	}

}
