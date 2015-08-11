package org.cru.migration.domain;

import org.joda.time.DateTime;

public class CssRelayUser
{
	private String ssoguid;
	private String username;
	private String password;
	private DateTime lastChanged;

	private String question;
	private String answer;
	private DateTime createdDate;

	public CssRelayUser()
	{
	}

	public String getSsoguid()
	{
		return ssoguid;
	}

	public void setSsoguid(String ssoguid)
	{
		this.ssoguid = ssoguid.toUpperCase();
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username.toLowerCase();
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public DateTime getLastChanged()
	{
		return lastChanged;
	}

	public void setLastChanged(DateTime lastChanged)
	{
		this.lastChanged = lastChanged;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(final String question) {
		this.question = question;
	}

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(final String answer) {
        this.answer = answer;
    }

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final DateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String toString() {
        return "CssRelayUser{" +
                "ssoguid='" + ssoguid + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", lastChanged=" + lastChanged +
                ", question='" + question + '\'' +
                ", answer='" + answer + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }

    @Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CssRelayUser relayUser = (CssRelayUser) o;

		if (ssoguid != null ? !ssoguid.equals(relayUser.ssoguid) : relayUser.ssoguid != null) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return ssoguid != null ? ssoguid.hashCode() : 0;
	}
}
