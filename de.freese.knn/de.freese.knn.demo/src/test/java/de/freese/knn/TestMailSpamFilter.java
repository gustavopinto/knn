/**
 * 11.06.2008
 */
package de.freese.knn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import de.freese.knn.net.NeuralNet;
import de.freese.knn.net.layer.hidden.SigmoidLayer;
import de.freese.knn.net.layer.input.InputLayer;
import de.freese.knn.net.layer.output.OutputLayer;
import de.freese.knn.net.math.forkjoin.ForkJoinKnnMath;
import de.freese.knn.net.trainer.ITrainingInputSource;
import de.freese.knn.net.trainer.LoggerNetTrainerListener;
import de.freese.knn.net.trainer.NetTrainer;

/**
 * Klasse zum Test des BinaryPersisters.
 * 
 * @author Thomas Freese
 */
public class TestMailSpamFilter implements ITrainingInputSource
{
	/**
	 * @param args String[]
	 * @throws Exception Falls was schief geht.
	 */
	public static void main(final String[] args) throws Exception
	{
		TestMailSpamFilter spamFilter = new TestMailSpamFilter();
		// spamFilter.cleanUp();

		NeuralNet neuralNetwork = new NeuralNet(new ForkJoinKnnMath());
		neuralNetwork.addLayer(new InputLayer(spamFilter.token.size()));
		neuralNetwork.addLayer(new SigmoidLayer(20000));
		neuralNetwork.addLayer(new OutputLayer(1));
		neuralNetwork.connectLayer();
		double teachFactor = 0.5D;
		double momentum = 0.5D;
		double maximumError = 0.05D;
		int maximumIteration = 10000;

		NetTrainer trainer = new NetTrainer(teachFactor, momentum, maximumError, maximumIteration);
		// trainer.addNetTrainerListener(new PrintStreamNetTrainerListener(System.out));
		trainer.addNetTrainerListener(new LoggerNetTrainerListener());
		trainer.train(neuralNetwork, spamFilter);

		spamFilter.closeDataSource();
	}

	/**
	 * 
	 */
	private JdbcTemplate jdbcTemplate = null;

	/**
	 * 
	 */
	private List<Map<String, Object>> messages = null;

	/**
	 * 
	 */
	private List<String> token = null;

	/**
	 * Erstellt ein neues {@link TestMailSpamFilter} Object.
	 */
	public TestMailSpamFilter()
	{
		super();

		SingleConnectionDataSource ds = new SingleConnectionDataSource();
		ds.setDriverClassName("com.mysql.jdbc.Driver");
		ds.setUrl("jdbc:mysql://nas/tommy?user=tommy&password=tommy");
		ds.setSuppressClose(true);

		this.jdbcTemplate = new JdbcTemplate(ds);

		this.messages = this.jdbcTemplate.queryForList("select message_id, is_spam from message");
		this.token = this.jdbcTemplate.queryForList("select token from token order by token", String.class);
	}

	/**
	 * 
	 */
	public void cleanUp()
	{
		// Entfernen aller Token aus gleichen Zeichen.
		// a-z
		for (char c = 97; c <= 122; c++)
		{
			for (int i = 3; i < 10; i++)
			{
				String t = StringUtils.repeat(c, i);
				t = "%" + t + "%";
				int deleted = this.jdbcTemplate.update("delete from message_token where token like ?", t);
				deleted += this.jdbcTemplate.update("delete from token where token like ?", t);

				System.out.printf("%s: %d deleted%n", t, deleted);
			}
		}

		// select count(*), is_spam from message group by is_spam
	}

	/**
	 * 
	 */
	public void closeDataSource()
	{
		DataSource dataSource = this.jdbcTemplate.getDataSource();

		if (dataSource instanceof SingleConnectionDataSource)
		{
			((SingleConnectionDataSource) dataSource).destroy();
		}

		this.jdbcTemplate = null;
	}

	/**
	 * @see de.freese.knn.net.trainer.ITrainingInputSource#getInputAt(int)
	 */
	@Override
	public double[] getInputAt(final int index)
	{
		String messageID = (String) this.messages.get(index).get("MESSAGE_ID");

		final double[] input = new double[this.token.size()];
		Arrays.fill(input, 0.0D);

		this.jdbcTemplate.query("select token from message_token where message_id = ?", new ResultSetExtractor<Void>()
		{
			/**
			 * @see org.springframework.jdbc.core.ResultSetExtractor#extractData(java.sql.ResultSet)
			 */
			@Override
			public Void extractData(final ResultSet rs) throws SQLException, DataAccessException
			{
				while (rs.next())
				{
					int index = TestMailSpamFilter.this.token.indexOf(rs.getString("token"));
					input[index] = 1.0D;
				}

				return null;
			}
		}, messageID);

		// token: 10184
		// SELECT * FROM message_token WHERE message_id = '<001b01ce0f36$5148a8d0$f3d9fa70$@profcon.de>' ORDER BY token
		//
		// select t.token as token, IF(m.is_spam = 0, 1, 0) as spam from token t
		// left outer join message_token mt on mt.token = t.token
		// left outer join message m on m.message_id = mt.message_id and mt.message_id = '<001b01ce0f36$5148a8d0$f3d9fa70$@profcon.de>'
		// group by token, spam
		// order by token asc

		return input;
	}

	/**
	 * @see de.freese.knn.net.trainer.ITrainingInputSource#getOutputAt(int)
	 */
	@Override
	public double[] getOutputAt(final int index)
	{
		Boolean isSpam = (Boolean) this.messages.get(index).get("IS_SPAM");
		double[] output = new double[]
		{
			isSpam ? 1.0D : 0.0D
		};

		return output;
	}

	/**
	 * @see de.freese.knn.net.trainer.ITrainingInputSource#getSize()
	 */
	@Override
	public int getSize()
	{
		return this.messages.size();
	}
}
